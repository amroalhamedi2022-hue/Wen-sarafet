#!/usr/bin/env python3
"""Regression for mixed-currency selection without rewriting any FX ledger formula."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
s=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
form=s.split("private void showAddTransaction() {",1)[1].split("private void ",1)[0]
fx=s.split("private void showFxTransferDialog()",1)[1].split("private View detailLine(",1)[0]
assert "FinanceRules.TRANSFER.equals(selectedType[0]) || a.currency.equals(selectedCurrency)" in form
assert "refreshAccounts.run();" in form.split("typeGroup.setOnCheckedChangeListener(",1)[1].split(
    "currency.setOnItemSelectedListener(",1)[0]
assert "source.currency" in form.split("String transactionCurrency=",1)[1].split(
    "db.addTransaction(",1)[0]
assert "if(!dest.currency.equals(source.currency))" in form
assert "showFxTransferDialog(source.id,dest.id,amount.getText().toString())" in form
assert "String transactionCurrency=FinanceRules.TRANSFER.equals(selectedType[0])" in form
assert "currency.setVisibility(isTransfer ? View.GONE : View.VISIBLE)" in form
assert "currencyLabel.setVisibility(isTransfer ? View.GONE : View.VISIBLE)" in form
assert "private void showFxTransferDialog(Long preferredSourceId,Long preferredDestinationId,String initialAmount)" in fx
assert "if(initialAmount!=null)amount.setText(initialAmount)" in fx
assert "if(accounts.get(i).id==preferredSourceId){source.setSelection(i);break;}" in fx
assert "if(accounts.get(i).id==preferredDestinationId){destination.setSelection(i);break;}" in fx
assert "db.addFxTransfer(" in fx
# Do not bypass rate provenance or original dedicated FX calculation.
assert "FxTransferMath.destinationMinor(" in fx
print("MixedCurrencyTransferUICheck PASS: all-account picker and original FX-rate confirmation preserved")
