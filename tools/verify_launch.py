"""Wait for rendered onboarding rather than photographing Android's launch splash."""
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=20)

for attempt in range(12):
    try:
        adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml')
        xml=adb('shell', 'cat', '/sdcard/window.xml').decode()
        nodes=ET.fromstring(xml).iter('node')
        if any('A little magic.' in node.get('text', '') for node in nodes):
            Path('app/build/reports/onboarding.png').write_bytes(adb('exec-out', 'screencap', '-p'))
            print('Onboarding UI rendered successfully.')
            break
    except (subprocess.SubprocessError, ET.ParseError):
        # Android may expose no accessibility root while leaving its launch splash.
        pass
    time.sleep(3)
else:
    print(adb('logcat','-d','-s','AndroidRuntime:E').decode())
    raise RuntimeError('Onboarding did not render within the launch window')
