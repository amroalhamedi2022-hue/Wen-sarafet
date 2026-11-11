"""No-network FX form contract: only owner-entered exchange rates may be used."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=(root/'app/src/main/java/app/wensarafet/tracker/MainActivity.java').read_text()
fx=main.split('    private void showFxTransferDialog() {',1)[1].split('    private void showTransactionTransferDialog',1)[0]
assert 'new Thread(' not in fx
assert 'BopRateFetcher' not in fx
assert 'FxRateRefreshPolicy' not in fx
assert 'onlineSelected' not in fx
assert 'new SimpleItemSelected(loadSavedRate)' in fx
assert 'fx_manual_rates' in fx
assert 'FxTransferMath.destinationMinor(' in fx
assert '"MANUAL",System.currentTimeMillis()' in fx
assert 'تحديث أونلاين' not in fx
print('FX form source contract PASS: manual rate only, no online quote')
