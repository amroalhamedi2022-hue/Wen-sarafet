package app.wensarafet.tracker;
public final class JawwalFriendWidget extends QuickActionWidgetProvider {
    @Override protected String action() { return "jawwal_friend"; }
    @Override protected String title() { return "Jawwal Pay"; }
    @Override protected String subtitle() { return "صديق"; }
    @Override protected int iconResource() { return R.drawable.icon_jawwal_pay_mark; }
    @Override protected String brandWord() { return "PAY"; }
}
