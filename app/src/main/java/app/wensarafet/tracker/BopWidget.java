package app.wensarafet.tracker;
public final class BopWidget extends QuickActionWidgetProvider {
    @Override protected String action() { return "bop"; }
    @Override protected String title() { return "Bank Of Palestine"; }
    @Override protected String subtitle() { return "USSD"; }
    @Override protected int iconResource() { return R.drawable.icon_bop_mark; }
}
