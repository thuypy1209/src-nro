import subprocess
import json

MYSQL_BIN = r"C:\xampp\mysql\bin\mysql.exe"

def run_query(sql):
    cmd = [MYSQL_BIN, "-u", "root", "-e", sql]
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise Exception(f"MySQL error: {result.stderr}")
    return result.stdout or ""

def execute_sql(sql):
    cmd = [MYSQL_BIN, "-u", "root", "nro", "-e", sql]
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise Exception(f"MySQL error: {result.stderr}")

def create_empty_items(count):
    items = []
    for _ in range(count):
        items.append({
            "temp_id": -1,
            "quantity": 0,
            "create_time": 0,
            "option": []
        })
    return items

def main():
    print("=== BẮT ĐẦU CHUYỂN ĐỔI DỮ LIỆU SANG CHUẨN KHANHDTK ===")

    # 1. Đồng bộ account IDs với player.account_id
    print("1. Đồng bộ bảng account...")
    acc_map = [
        (3728, '1'),
        (3731, 'mikloq'),
        (3732, 'shirokuma'),
        (3733, 'minhbest'),
        (3734, 'minhbest1'),
        (3735, 'tehau'),
        (3736, 'minhbest2'),
        (3738, 'tqh'),
        (3739, 'tehau2')
    ]
    for target_id, uname in acc_map:
        execute_sql(f"DELETE FROM account WHERE id = {target_id} AND username != '{uname}';")
        res = run_query(f"SELECT id FROM nro.account WHERE username = '{uname}';")
        lines = [line.strip() for line in res.strip().split("\n") if line.strip() and line.strip() != 'id']
        if lines:
            execute_sql(f"UPDATE account SET id = {target_id}, server_login = 1 WHERE username = '{uname}';")
        else:
            execute_sql(f"INSERT INTO account (id, username, password, server_login, active) VALUES ({target_id}, '{uname}', '1', 1, 1);")
        print(f"   Account '{uname}' -> id={target_id}, pass='1'")

    execute_sql("UPDATE account SET server_login = 1;")

    # 2. Lấy danh sách 9 người chơi
    player_ids = [1367, 1371, 1374, 1375, 1377, 1378, 1379, 1380, 1381]
    for pid in player_ids:
        out = run_query(f"SELECT * FROM nro.player WHERE id = {pid}\\G")
        fields = {}
        for line in out.split("\n"):
            line = line.strip()
            if not line or line.startswith("*"):
                continue
            if ":" in line:
                k, v = line.split(":", 1)
                fields[k.strip()] = v.strip()

        pname = fields.get('name', '')
        print(f"Chuyển đổi dữ liệu cho player: {pname} (id={pid})...")

        updates = []
        updates.append("MaBaoVe = '[0,0]'")
        updates.append("checkNhanQua = '[1,1]'")
        updates.append("data_mabu_egg = '{}'")
        updates.append("enemies = '[]'")
        updates.append("friends = '[]'")

        # items_body -> ít nhất 13 slot
        try:
            body = json.loads(fields.get('items_body', '[]'))
            if not body or len(body) < 13:
                body = create_empty_items(13)
                updates.append(f"items_body = '{json.dumps(body)}'")
        except:
            updates.append(f"items_body = '{json.dumps(create_empty_items(13))}'")

        # items_bag -> ít nhất 20 slot
        try:
            bag = json.loads(fields.get('items_bag', '[]'))
            if not bag or len(bag) < 20:
                bag = create_empty_items(20)
                updates.append(f"items_bag = '{json.dumps(bag)}'")
        except:
            updates.append(f"items_bag = '{json.dumps(create_empty_items(20))}'")

        # items_box -> ít nhất 20 slot
        try:
            box = json.loads(fields.get('items_box', '[]'))
            if not box or len(box) < 20:
                box = create_empty_items(20)
                updates.append(f"items_box = '{json.dumps(box)}'")
        except:
            updates.append(f"items_box = '{json.dumps(create_empty_items(20))}'")

        # items_box_lucky_round -> ít nhất 110 slot
        try:
            lr = json.loads(fields.get('items_box_lucky_round', '[]'))
            if not lr or len(lr) < 110:
                lr = create_empty_items(110)
                updates.append(f"items_box_lucky_round = '{json.dumps(lr)}'")
        except:
            updates.append(f"items_box_lucky_round = '{json.dumps(create_empty_items(110))}'")

        # data_point
        try:
            dp = json.loads(fields.get('data_point', '[]'))
            if len(dp) == 14 and int(dp[1]) > 2000000000:
                limit_power = dp[0]
                power = dp[1]
                tiem_nang = dp[2]
                stamina = dp[3]
                max_stamina = dp[4]
                hp = dp[5]
                mp = dp[6]
                damg = dp[7]
                defg = dp[8]
                critg = dp[9]
                hpg = dp[12]
                mpg = dp[13]
                new_dp = [0, mp, mpg, critg, limit_power, stamina, hp, defg, tiem_nang, max_stamina, damg, power, hpg]
                updates.append(f"data_point = '{json.dumps(new_dp)}'")
                print(f"   -> data_point converted: power={power}, tn={tiem_nang}, hp={hp}/{hpg}")
        except Exception as e:
            pass

        # data_magic_tree
        try:
            dmt = json.loads(fields.get('data_magic_tree', '[]'))
            if len(dmt) == 5 and int(dmt[3]) > 1000000000000:
                level = dmt[0]
                curr_pea = dmt[1]
                is_upgr = dmt[2]
                last_harv = dmt[3]
                last_upgr = dmt[4]
                new_dmt = [is_upgr, last_upgr, level, last_harv, curr_pea]
                updates.append(f"data_magic_tree = '{json.dumps(new_dmt)}'")
        except Exception as e:
            pass

        # data_task
        try:
            dt = json.loads(fields.get('data_task', '[]'))
            if len(dt) == 4:
                new_dt = [dt[2], dt[0], dt[1]]
                updates.append(f"data_task = '{json.dumps(new_dt)}'")
        except Exception as e:
            pass

        # skills
        try:
            sk_raw = json.loads(fields.get('skills', '[]'))
            new_skills = []
            for item in sk_raw:
                if isinstance(item, str):
                    s_arr = json.loads(item)
                    new_skills.append([s_arr[0], s_arr[2], s_arr[1]])
                elif isinstance(item, list):
                    new_skills.append(item)
            if new_skills:
                updates.append(f"skills = '{json.dumps(new_skills)}'")
        except Exception as e:
            pass

        # pet
        try:
            pinfo_raw = fields.get('pet_info', '{}')
            if pinfo_raw.startswith('['):
                pinfo_arr = json.loads(pinfo_raw)
                gender = pinfo_arr[0]
                is_mabu = 1 if pinfo_arr[1] == 1 else 0
                name = pinfo_arr[2]
                type_fusion = pinfo_arr[3]
                left_fusion = pinfo_arr[4]
                status = pinfo_arr[5]
                new_pinfo = {
                    "is_SuperPicolo": 0, "is_CellBao": 0, "gender": gender,
                    "is_mabu": is_mabu, "is_FideTrau": 0, "name": name,
                    "is_BillNhi": 0, "type_fusion": type_fusion,
                    "left_fusion": left_fusion, "is_Bulo": 0, "status": status
                }
                updates.append(f"pet_info = '{json.dumps(new_pinfo)}'")

                ppoint_arr = json.loads(fields.get('pet_point', '[]'))
                if len(ppoint_arr) >= 12:
                    new_ppoint = {
                        "limit_power": ppoint_arr[0], "power": ppoint_arr[1], "tiem_nang": ppoint_arr[2],
                        "stamina": ppoint_arr[3], "max_stamina": ppoint_arr[4], "hp": ppoint_arr[5],
                        "mp": ppoint_arr[6], "damg": ppoint_arr[7], "defg": ppoint_arr[8],
                        "critg": ppoint_arr[9], "hpg": ppoint_arr[10], "mpg": ppoint_arr[11]
                    }
                    updates.append(f"pet_point = '{json.dumps(new_ppoint)}'")

                pskill_raw = json.loads(fields.get('pet_skill', '[]'))
                new_pskill = []
                for s in pskill_raw:
                    if isinstance(s, str):
                        s_arr = json.loads(s)
                        new_pskill.append([s_arr[0], s_arr[1]])
                    elif isinstance(s, list):
                        new_pskill.append(s[:2])
                while len(new_pskill) < 4:
                    new_pskill.append([-1, 0])
                updates.append(f"pet_skill = '{json.dumps(new_pskill)}'")
                updates.append(f"pet_body = '{json.dumps(create_empty_items(6))}'")
        except Exception as e:
            pass

        # achivements
        try:
            ach_raw = json.loads(fields.get('achivements', '[]'))
            new_ach = []
            for i, a in enumerate(ach_raw):
                count = 0
                finish = 0
                if isinstance(a, str):
                    a_arr = json.loads(a)
                    count = int(a_arr[0])
                    finish = 1 if a_arr[1] else 0
                elif isinstance(a, dict):
                    count = int(a.get('count', 0))
                    finish = 1 if a.get('finish', 0) in (1, True, "1", "true") else 0
                new_ach.append({"id": i, "count": count, "finish": finish, "receive": finish})
            while len(new_ach) < 16:
                new_ach.append({"id": len(new_ach), "count": 0, "finish": 0, "receive": 0})
            updates.append(f"achivements = '{json.dumps(new_ach)}'")
        except Exception as e:
            pass

        if updates:
            sql_update = f"UPDATE player SET {', '.join(updates)} WHERE id = {pid};"
            execute_sql(sql_update)
            print(f"   -> Đã cập nhật thành công {len(updates)} cột cho {pname} (id={pid})!")

    print("\n=== HOÀN TẤT CHUYỂN ĐỔI TOÀN BỘ CƠ SỞ DỮ LIỆU ===")

if __name__ == '__main__':
    main()
