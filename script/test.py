import requests
import uuid
import random
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://127.0.0.1:8080"

def create_user(name):
    res = requests.post(f"{BASE_URL}/user", json={"name": name})
    return res.json()['data']

def get_user(name):
    res = requests.get(f"{BASE_URL}/user/{name}")
    return res.json()['data']

def send_payment(user_id, amount, p_type, idempotency_key):
    headers = {"Idempotency-Key": idempotency_key}
    payload = {
        "userId": user_id,
        "amount": amount,
        "type": p_type
    }
    try:
        res = requests.post(f"{BASE_URL}/transaction", json=payload, headers=headers)
        return res.status_code, res.json()
    except Exception as e:
        return 500, str(e)

def run_test_a():
    print("\n=== 테스트 A 시작: 동시 입금 1000건 (모두 다른 멱등키) ===")
    user = create_user("Juhyeong")
    user_id = user['id']
    
    tasks = []
    expected_total = 0
    
    # 100개씩 10번 = 총 1000번
    for _ in range(1000):
        amount = random.randint(100, 1000)
        tasks.append({
            "amount": amount,
            "key": str(uuid.uuid4())
        })
        expected_total += amount

    with ThreadPoolExecutor(max_workers=50) as executor:
        futures = [executor.submit(send_payment, user_id, t['amount'], "DEPOSIT", t['key']) for t in tasks]
        for future in as_completed(futures):
            future.result()

    final_user = get_user("Juhyeong")
    print(f"예상 잔액: {expected_total}")
    print(f"실제 잔액: {final_user['balance']}")
    print("결과:", "✅ 성공" if float(final_user['balance']) == float(expected_total) else "❌ 실패")

def run_test_b():
    print("\n=== 테스트 B 시작: 동시 입금 1000건 (5개씩 멱등키 중복) ===")
    user = create_user("JuhyeongLee")
    user_id = user['id']
    
    tasks = []
    expected_total = 0
    
    # 총 1000개 요청 중, 200개의 고유 키 생성 (각 키당 5번씩 쏨)
    for _ in range(200):
        amount = random.randint(100, 1000)
        unique_key = str(uuid.uuid4())
        expected_total += amount # 멱등키가 같으므로 한 번만 더해짐
        
        for _ in range(5):
            tasks.append({"amount": amount, "key": unique_key})

    # 랜덤하게 섞어서 동시성 극대화
    random.shuffle(tasks)

    with ThreadPoolExecutor(max_workers=50) as executor:
        futures = [executor.submit(send_payment, user_id, t['amount'], "DEPOSIT", t['key']) for t in tasks]
        for future in as_completed(futures):
            future.result()

    final_user = get_user("JuhyeongLee")
    print(f"예상 잔액 (중복 제외): {expected_total}")
    print(f"실제 잔액: {final_user['balance']}")
    print("결과:", "✅ 성공" if float(final_user['balance']) == float(expected_total) else "❌ 실패")

if __name__ == "__main__":
    run_test_a()
    run_test_b()