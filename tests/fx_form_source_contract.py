"""Static regression guards for the source-only FX refresh integration.

These DO NOT replace Android instrumentation tests. They make it harder to remove
explicit confirmation or silently modify entered manual exchange rates.
"""
from pathlib import Path

main = (Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/MainActivity.java').read_text()
fetch = (Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/BopRateFetcher.java').read_text()
method = main.split('    private void showFxTransferDialog() {',1)[1].split('    private void showTransactionTransferDialog',1)[0]
assert 'onPairChanged[0]=()->{' in method
assert 'FxRateRefreshPolicy.needsDailyRefresh(' in method
assert 'source.setOnItemSelectedListener(new SimpleItemSelected(loadSavedRate));' in method
assert 'destination.setOnItemSelectedListener(new SimpleItemSelected(loadSavedRate));' in method
assert 'if(onPairChanged[0]!=null)onPairChanged[0].run();' in method
assert 'if(manual)confirmCachedRate.run();' in method
assert 'if(autoAttemptedThisDialog[0] || loadingQuote[0])return;' in method
assert 'new AlertDialog.Builder(this).setTitle("تسعيرة بنك فلسطين (شراء الدولار)")' in method
assert 'if(!dialogVisible[0])return;' in method
assert 'onlineSelected?"BOP_INDICATIVE_BUY_USER_CONFIRMED":"MANUAL"' in method
assert 'https://www.bop.ps/personal/exchange' in fetch
print('FX form source contract: passed (static checks only)')
