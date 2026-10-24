package app.wensarafet.tracker;
public final class PalPayWidget extends QuickActionWidgetProvider {
    @Override protected String action() { return "palpay"; }
    @Override protected String title() { return "PalPay"; }
    @Override protected String subtitle() { return "تحويل"; }
    @Override protected int iconResource() { return R.drawable.icon_palpay_mark; }
}
