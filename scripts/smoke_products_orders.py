"""商品与多商品订单的定向 API 验证；会创建保留测试数据，请明确指定本地验证地址。"""
import argparse
import concurrent.futures
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', required=True)
parser.add_argument('--result-file', type=Path, required=True)
parser.add_argument('--legacy-order-id', type=int)
args = parser.parse_args()
assert urllib.parse.urlparse(args.base_url).hostname in {'localhost', '127.0.0.1'}, 'Only localhost verification is supported'
base = args.base_url.rstrip('/') + '/api'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
checks = []

def call(method, path, body=None, expected=200):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(base + path, method=method, data=data, headers={'Content-Type': 'application/json'})
    try:
        with opener.open(request, timeout=30) as response:
            status, result = response.status, json.load(response)
    except urllib.error.HTTPError as error:
        status, result = error.code, json.load(error)
    assert status == expected, f'{method} {path}: expected {expected}, got {status}, {result}'
    return result

def passed(name):
    checks.append(name)
    print('PASS ' + name, flush=True)

if args.legacy_order_id:
    legacy = call('GET', f'/orders/{args.legacy_order_id}')
    assert legacy['totalAmount'] == '59.70' and legacy['items'][0]['source'] == 'LEGACY'
    assert legacy['items'][0]['productId'] is None and legacy['items'][0]['productName'] == '历史键盘'
    passed('V1历史订单迁移为快照明细，原金额保留')

p1 = call('POST', '/products', {'type': ' 办公设备 ', 'name': ' 机械键盘 ', 'price': '299.00'}, 201)
p2 = call('POST', '/products', {'type': '办公设备', 'name': '无线鼠标', 'price': '89.00'}, 201)
assert p1['type'] == '办公设备' and p1['name'] == '机械键盘' and p1['price'] == '299.00' and p1['enabled']
passed('商品新增、去除首尾空白、默认启用与金额字符串')
order = call('POST', '/orders', {'items': [{'productId': p2['id'], 'quantity': 1}, {'productId': p1['id'], 'quantity': 2}]}, 201)
assert order['totalAmount'] == '687.00' and order['totalQuantity'] == 3 and order['productCount'] == 2
assert [item['lineTotal'] for item in order['items']] == ['598.00', '89.00']
passed('多商品精确合计687.00，返回完整明细')
call('PUT', f'/products/{p1["id"]}', {'type': '电脑外设', 'name': '机械键盘新款', 'price': '399.00'})
old = call('GET', f'/orders/{order["id"]}')
assert old['totalAmount'] == '687.00' and old['items'][0]['productName'] == '机械键盘'
assert old['items'][0]['productType'] == '办公设备' and old['items'][0]['unitPrice'] == '299.00'
new = call('POST', '/orders', {'items': [{'productId': p1['id'], 'quantity': 1}]}, 201)
assert new['totalAmount'] == '399.00' and new['items'][0]['productType'] == '电脑外设'
passed('编辑商品后旧单三项快照不变，新单使用新配置')
call('PUT', f'/products/{p1["id"]}/status', {'enabled': False})
assert p1['id'] not in {item['id'] for item in call('GET', '/products?enabled=true')}
before_ids = [item['id'] for item in call('GET', '/orders')]
for product_id in [p1['id'], 999999999]:
    call('POST', '/orders', {'items': [{'productId': p2['id'], 'quantity': 1}, {'productId': product_id, 'quantity': 1}]}, 400)
assert [item['id'] for item in call('GET', '/orders')] == before_ids
assert call('GET', f'/orders/{order["id"]}')['items'][0]['unitPrice'] == '299.00'
passed('停用筛选、缺失/停用商品整单失败、旧单仍可读')
invalid_orders = [
    {}, {'items': []}, {'items': [None]}, {'items': [{'productId': p2['id'], 'quantity': 1.5}]},
    {'items': [{'productId': p2['id'], 'quantity': 0}]},
    {'items': [{'productId': p2['id'], 'quantity': 1001}]},
    {'items': [{'productId': p2['id'], 'quantity': 1, 'unitPrice': '0.01'}]},
    {'itemName': '旧格式', 'quantity': 1, 'unitPrice': '0.01'},
]
for body in invalid_orders:
    assert call('POST', '/orders', body, 400)['message']
assert [item['id'] for item in call('GET', '/orders')] == before_ids
passed('空明细、数量边界、小数、客户端价格与旧格式均拒绝')
for body in [{'type': ' ', 'name': '无效', 'price': '1.00'}, {'type': '办公', 'name': '无效', 'price': '1.001'},
             {'type': '办公', 'name': '无效', 'price': '0.00'}, {'type': 'x' * 41, 'name': '无效', 'price': '1.00'}]:
    call('POST', '/products', body, 400)
call('PUT', f'/products/{p2["id"]}/status', {}, 400)
passed('商品类型、价格范围及状态必填校验')
call('PUT', f'/products/{p1["id"]}/status', {'enabled': True})
assert p1['id'] in {item['id'] for item in call('GET', '/products?enabled=true')}
merged = call('POST', '/orders', {'items': [{'productId': p2['id'], 'quantity': 1}, {'productId': p2['id'], 'quantity': 2}]}, 201)
assert len(merged['items']) == 1 and merged['items'][0]['quantity'] == 3 and merged['totalAmount'] == '267.00'
call('POST', '/orders', {'items': [{'productId': p2['id'], 'quantity': 1000}, {'productId': p2['id'], 'quantity': 1}]}, 400)
passed('重新启用与重复商品合并，合并后数量上限仍生效')
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    cancelled = list(pool.map(lambda _: call('POST', f'/orders/{order["id"]}/cancel'), range(8)))
assert all(item['status'] == 'CANCELLED' for item in cancelled)
assert len(call('GET', f'/orders/{order["id"]}')['items']) == 2
passed('8路并发取消幂等，已取消订单明细保留')
call('GET', '/orders/999999999', expected=404)
call('PUT', '/products/999999999', {'type': '办公', 'name': '不存在', 'price': '1.00'}, 404)
passed('不存在订单/商品返回404')
large = call('POST', '/products', {'type': '金额边界', 'name': '价格上限商品', 'price': '99999999.99'}, 201)
large_order = call('POST', '/orders', {'items': [{'productId': large['id'], 'quantity': 1000}]}, 201)
assert large_order['totalAmount'] == '99999999990.00'
passed('价格与数量上限乘积精确返回')
race = call('POST', '/products', {'type': '并发10', 'name': '商品10', 'price': '10.00'}, 201)
def concurrent_operation(index):
    if index < 2:
        price = [20, 30][index]
        call('PUT', f'/products/{race["id"]}', {'type': f'并发{price}', 'name': f'商品{price}', 'price': f'{price}.00'})
        return None
    return call('POST', '/orders', {'items': [{'productId': race['id'], 'quantity': 3}]}, 201)
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    race_results = list(pool.map(concurrent_operation, range(6)))
for result in filter(None, race_results):
    line = result['items'][0]
    assert (line['productName'], line['productType'], line['unitPrice'], line['lineTotal']) in {
        ('商品10', '并发10', '10.00', '30.00'), ('商品20', '并发20', '20.00', '60.00'), ('商品30', '并发30', '30.00', '90.00')}
passed('并发编辑与下单保持同一版本的名称、类型、价格快照')
if args.legacy_order_id:
    call('POST', f'/orders/{args.legacy_order_id}/cancel')
    assert call('GET', f'/orders/{args.legacy_order_id}')['totalAmount'] == '59.70'
    passed('迁移历史订单仍可取消并保留金额')
result = {'base_url': args.base_url, 'checks': checks, 'passed': len(checks), 'order_id': order['id'],
    'new_order_id': new['id'], 'product_ids': [p1['id'], p2['id']], 'legacy_order_id': args.legacy_order_id,
    'full_regression': '未执行', 'data': '测试记录保留在指定验证环境'}
args.result_file.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'passed': len(checks), 'order_id': order['id']}, ensure_ascii=False))
