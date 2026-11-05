import subprocess
import os

server_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server'
jar_path = os.path.join(server_dir, 'Server.jar')
sources_file = os.path.join(server_dir, 'sources_to_compile.txt')
build_temp = os.path.join(server_dir, 'target', 'classes')
cp = f"{jar_path};{os.path.join(server_dir, 'lib', '*')}"

cmd = ['javac', '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '-cp', cp, '-d', build_temp, f'@{sources_file}']
res = subprocess.run(cmd, cwd=server_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

errors = res.stderr.splitlines()
print(f"Total error lines: {len(errors)}")

# Group errors by file
file_errors = {}
for e in errors:
    if ': error:' in e:
        parts = e.split(': error:')
        fname = parts[0].split(':')[0]
        err_msg = parts[1].strip()
        file_errors.setdefault(fname, []).append(err_msg)

print(f"Unique files with errors: {len(file_errors)}")
for f, errs in file_errors.items():
    print(f"\n{f} ({len(errs)} errors):")
    for em in errs[:3]:
        print(f"  - {em}")
