import requests
import uuid
import random
import time
import json
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://127.0.0.1:8080"
IDEM_HEADER = "Idempotency-Key"
BLOCKING_HEADER = "blockingSecondsForTest"

test_logs = []

# --- 인터랙티브 출력 유틸리티 ---

def print_step(title, description):
    print(f"\n🚀 {title}")
    print(f"   💡 목적: {description}")

def update_progress(current, total, prefix="진행"):
    # 퍼센트 없이 [n/m] 형식으로만 출력
    sys.stdout.write(f"\r   {prefix}: [{current}/{total}] 완료! 🏃")
    sys.stdout.flush()

def countdown(seconds, msg="대기 중"):
    for i in range(seconds, 0, -1):
        sys.stdout.write(f"\r   ⏳ {msg}: {i}초 남음... ")
        sys.stdout.flush()
        time.sleep(1)
    print(f"\r   ✅ {msg} 완료!                          ")

# --- 통신 함수 ---

def log_interaction(test_name, method, url, headers, payload, status_code, response):
    log_entry = {
        "test_name": test_name,
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "request": {"method": method, "url": url, "headers": headers, "body": payload},
        "response": {"status_code": status_code, "body": response}
    }
    test_logs.append(log_entry)

def create_user(test_name, name):
    idempotency_key = str(uuid.uuid4())
    email = f"{name.lower()}@{uuid.uuid4().hex[:6]}.com"
    random_number = random.randint(1000, 9999)
    payload = {"name": name + str(random_number), "email": email}
    headers = {IDEM_HEADER: idempotency_key}
    
    res = requests.post(f"{BASE_URL}/user", json=payload, headers=headers)
    response_json = res.json() if res.status_code == 200 else res.text
    log_interaction(test_name, "POST", f"{BASE_URL}/user", headers, payload, res.status_code, response_json)
    
    if res.status_code != 200:
        raise Exception(f"유저 생성 실패: {res.text}")
    return response_json.get('data')

def send_payment(test_name, user_id, amount, p_type, idempotency_key, blocking_s=0):
    headers = {}
    if idempotency_key: headers[IDEM_HEADER] = idempotency_key
    if blocking_s > 0: headers[BLOCKING_HEADER] = str(blocking_s)
        
    payload = {"userId": user_id, "amount": amount, "type": p_type}
    try:
        res = requests.post(f"{BASE_URL}/transaction", json=payload, headers=headers)
        status_code = res.status_code
        try: response_data = res.json()
        except: response_data = res.text
        log_interaction(test_name, "POST", f"{BASE_URL}/transaction", headers, payload, status_code, response_data)
        return status_code, response_data
    except Exception as e:
        log_interaction(test_name, "POST", f"{BASE_URL}/transaction", headers, payload, 500, str(e))
        return 500, str(e)

# --- 테스트 시나리오 (A ~ F) ---

def run_test_a():
    print_step("테스트 A: 대규모 동시성 폭격", "1,000개의 고유 요청이 동시에 들어올 때 잔액의 무결성을 검증합니다.")
    user = create_user("Test A", "TesterA")
    tasks = [{"amount": 100, "key": str(uuid.uuid4())} for _ in range(1000)]
    
    count = 0
    with ThreadPoolExecutor(max_workers=50) as executor:
        futures = [executor.submit(send_payment, "Test A", user['id'], t['amount'], "DEPOSIT", t['key']) for t in tasks]
        for f in as_completed(futures):
            f.result()
            count += 1
            if count % 5 == 0: update_progress(count, 1000, "고유 요청 처리")
    
    final_res = requests.get(f"{BASE_URL}/user/{user['name']}").json().get('data')
    print(f"\n   📊 잔액 확인 -> 실제: {final_res['balance']} (예상: 100000.0)")
    assert float(final_res['balance']) == 100000.0
    print("   ✅ [성공] 동시성 제어가 완벽하게 작동합니다.")

def run_test_b():
    print_step("테스트 B: 끈질긴 중복 요청", "동일한 멱등키로 5번씩 보낸 요청이 단 1번만 처리되는지 확인합니다.")
    user = create_user("Test B", "TesterB")
    tasks = []
    for _ in range(200):
        key = str(uuid.uuid4())
        for _ in range(5): tasks.append({"amount": 100, "key": key})
    random.shuffle(tasks)

    count = 0
    with ThreadPoolExecutor(max_workers=50) as executor:
        futures = [executor.submit(send_payment, "Test B", user['id'], t['amount'], "DEPOSIT", t['key']) for t in tasks]
        for f in as_completed(futures):
            f.result()
            count += 1
            if count % 5 == 0: update_progress(count, 1000, "중복 요청 차단")
    
    final_res = requests.get(f"{BASE_URL}/user/{user['name']}").json().get('data')
    print(f"\n   📊 잔액 확인 -> 실제: {final_res['balance']} (예상: 20000.0)")
    assert float(final_res['balance']) == 20000.0
    print("   ✅ [성공] 멱등성에 의해 중복 입금이 차단되었습니다.")

def run_test_c():
    print_step("테스트 C: 스케줄러 청소 확인", "멱등 레코드가 삭제된 후 동일 키로 재입금이 가능한지 확인합니다.")
    user = create_user("Test C", "TesterC")
    keys = [str(uuid.uuid4()) for _ in range(500)]

    for i, key in enumerate(keys):
        send_payment("Test C", user['id'], 100, "DEPOSIT", key)
        if (i+1) % 10 == 0: update_progress(i+1, 500, "1차 요청 완료")
    print("")

    countdown(15, "스케줄러 청소 대기")

    for i, key in enumerate(keys):
        send_payment("Test C", user['id'], 100, "DEPOSIT", key)
        if (i+1) % 10 == 0: update_progress(i+1, 500, "2차 재요청")
    
    final_res = requests.get(f"{BASE_URL}/user/{user['name']}").json().get('data')
    print(f"\n   📊 잔액 확인 -> 실제: {final_res['balance']} (예상: 100000.0)")
    assert float(final_res['balance']) == 100000.0
    print("   ✅ [성공] 만료된 멱등 데이터는 재처리가 가능합니다.")

def run_test_d():
    print_step("테스트 D: 필수 헤더 누락", "멱등키(Idempotency-Key) 없이 요청했을 때 400 에러를 반환하는지 확인합니다.")
    user = create_user("Test D", "TesterD")
    status, _ = send_payment("Test D", user['id'], 1000, "DEPOSIT", None)
    
    update_progress(1, 1, "헤더 누락 검증")
    print(f"\n   📊 응답 코드 -> {status} (예상: 400)")
    assert status == 400
    print("   ✅ [성공] 헤더 누락 시 요청이 거부되었습니다.")

def run_test_e():
    print_step("테스트 E: 데이터 변조 시도", "동일한 키로 다른 Body 데이터를 보냈을 때 422 에러를 반환하는지 확인합니다.")
    user = create_user("Test E", "TesterE")
    key = str(uuid.uuid4())
    
    send_payment("Test E", user['id'], 100, "DEPOSIT", key)
    update_progress(1, 2, "1차 정상 전송")
    
    # 금액을 변경하여 전송
    status, _ = send_payment("Test E", user['id'], 999, "DEPOSIT", key)
    update_progress(2, 2, "2차 변조 전송")
    
    print(f"\n   📊 응답 코드 -> {status} (예상: 422)")
    assert status == 422
    print("   ✅ [성공] 데이터 불일치가 감지되어 차단되었습니다.")

def run_test_f():
    print_step("테스트 F: 처리 중 중복 요청", "첫 번째 요청이 처리 중일 때 같은 키로 온 요청에 409를 반환하는지 확인합니다.")
    user = create_user("Test F", "TesterF")
    key = str(uuid.uuid4())
    
    print("   -> 1차 요청 지연 전송 및 2차 요청 즉시 투입...")
    with ThreadPoolExecutor(max_workers=2) as executor:
        f1 = executor.submit(send_payment, "Test F", user['id'], 9, "DEPOSIT", key, blocking_s=0)
        time.sleep(1) # 서버가 PROCESSING 상태가 될 시간
        f2 = executor.submit(send_payment, "Test F", user['id'], 9, "DEPOSIT", key, blocking_s=0)
        
        s1, _ = f1.result()
        s2, _ = f2.result()
    
    update_progress(1, 1, "동시 요청 검증")
    print(f"\n   📊 응답 확인 -> 1차: {s1}, 2차(중복): {s2} (예상: 409)")
    assert s2 == 409
    print("   ✅ [성공] 진행 중인 요청에 대한 중복 접근을 차단했습니다.")

if __name__ == "__main__":
    print("="*65)
    print("       🛡️  입출금 시스템 멱등성(Idempotency) 통합 검증 엔진 🛡️")
    print("="*65)
    
    try:
        run_test_a()
        run_test_b()
        run_test_c()
        run_test_d()
        run_test_e()
        run_test_f()
        print("\n" + "✨" * 22)
        print("🎉 모든 테스트 통과!")
        print("✨" * 22)
    except Exception as e:
        print(f"\n\n❌ 테스트 중단: {e}")
    finally:
        with open("result.json", "w", encoding="utf-8") as f:
            json.dump(test_logs, f, indent=4, ensure_ascii=False)
        print(f"\n📄 전체 상세 로그 저장 완료: result.json")