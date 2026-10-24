"""定向验证订单创建、校验、查询与幂等取消；保留演示订单。"""
import argparse
import json
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8081")
    args = parser.parse_args()
    def call(path, method="GET", payload=None, expected=200):
        data = None if payload is None else json.dumps(payload).encode()
        request = urllib.request.Request(args.base_url + "/api" + path, data=data, method=method,
                                         headers={"Content-Type": "application/json"})
        try:
            response = urllib.request.urlopen(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            assert response.status == expected, f"{method} {path}: {response.status}, expected {expected}"
            return json.loads(response.read(), parse_float=Decimal)
    assert call("/health")["status"] == "UP"
    order = call("/orders", "POST", {"itemName": "演示机械键盘", "quantity": 3, "unitPrice": "19.90"}, 201)
    assert order["totalAmount"] == Decimal("59.70") and order["status"] == "NEW"
    call("/orders", "POST", {"itemName": " ", "quantity": 1, "unitPrice": "1.00"}, 400)
    call("/orders", "POST", {"itemName": "非法数量", "quantity": 0, "unitPrice": "1.00"}, 400)
    call("/orders", "POST", {"itemName": "小数数量", "quantity": 1.5, "unitPrice": "1.00"}, 400)
    call("/orders", "POST", {"itemName": "非法金额", "quantity": 1, "unitPrice": "1.001"}, 400)
    assert any(item["id"] == order["id"] for item in call("/orders"))
    first = call(f"/orders/{order['id']}/cancel", "POST")
    second = call(f"/orders/{order['id']}/cancel", "POST")
    assert first == second and first["status"] == "CANCELLED"
    call("/orders/9223372036854775807/cancel", "POST", expected=404)
    concurrent = call("/orders", "POST", {"itemName": "并发取消演示", "quantity": 1, "unitPrice": "10.00"}, 201)
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(lambda _: call(f"/orders/{concurrent['id']}/cancel", "POST"), range(8)))
    assert all(item == results[0] and item["status"] == "CANCELLED" for item in results)
    print(json.dumps({"result": "passed", "checks": 11, "order_id": order["id"],
                      "amount": str(order["totalAmount"]), "note": "演示订单已取消并保留"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
