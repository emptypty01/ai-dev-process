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
    product = call("/products", "POST", {"type": "演示商品", "name": "演示机械键盘", "price": "19.90"}, 201)
    order = call("/orders", "POST", {"items": [{"productId": product["id"], "quantity": 3}]}, 201)
    assert Decimal(order["totalAmount"]) == Decimal("59.70") and order["status"] == "NEW"
    call("/orders", "POST", {"items": []}, 400)
    call("/orders", "POST", {"items": [{"productId": product["id"], "quantity": 0}]}, 400)
    call("/orders", "POST", {"items": [{"productId": product["id"], "quantity": 1.5}]}, 400)
    call("/products", "POST", {"type": "演示商品", "name": "非法金额", "price": "1.001"}, 400)
    assert any(item["id"] == order["id"] for item in call("/orders"))
    first = call(f"/orders/{order['id']}/cancel", "POST")
    second = call(f"/orders/{order['id']}/cancel", "POST")
    assert first == second and first["status"] == "CANCELLED"
    call("/orders/9223372036854775807/cancel", "POST", expected=404)
    concurrent = call("/orders", "POST", {"items": [{"productId": product["id"], "quantity": 1}]}, 201)
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(lambda _: call(f"/orders/{concurrent['id']}/cancel", "POST"), range(8)))
    assert all(item == results[0] and item["status"] == "CANCELLED" for item in results)
    print(json.dumps({"result": "passed", "checks": 11, "order_id": order["id"],
                      "amount": str(order["totalAmount"]), "note": "演示订单已取消并保留"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
