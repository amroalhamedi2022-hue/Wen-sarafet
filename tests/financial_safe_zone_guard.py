#!/usr/bin/env python3
"""Hard freeze for financial Safe Zone.

Git blob identifiers are audited against source bytes. Future UI-only changes
do not require touching finance/db code, and any finance/security change
requires an explicit user request and corresponding baseline review.
"""
import hashlib
import re
from pathlib import Path

root=Path(__file__).resolve().parents[1]
baseline=root/"tests/financial_safe_zone_git_blobs.txt"
entries=[]
for line in baseline.read_text(encoding="utf-8").splitlines():
    if not line or line.startswith("#"):
        continue
    parts=line.split()
    assert len(parts)==2 and re.fullmatch(r"[0-9a-f]{40}",parts[0]), line
    entries.append((parts[0],parts[1]))
assert len(entries)>=7,"Core financial protection set is incomplete"
assert any(path.endswith("/FinanceDbHelper.java") for _,path in entries)
assert any(path.endswith("/NonNegativeBalanceRules.java") for _,path in entries)
assert len({path for _,path in entries})==len(entries)
for expected,path in entries:
    file=root/path
    assert file.is_file(), f"Protected financial file missing: {path}"
    data=file.read_bytes()
    prefix=f"blob {len(data)}\0".encode("ascii")
    actual=hashlib.sha1(prefix+data).hexdigest()
    assert actual==expected, (
        "FINANCIAL SAFE ZONE VIOLATED: "+path+
        " — money/debt/accounts are frozen until the owner explicitly requests modifications"
    )
print("FinancialSafeZone: protected finance engine source unchanged across this build")
