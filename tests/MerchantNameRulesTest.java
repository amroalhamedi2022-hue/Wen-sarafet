import app.wensarafet.tracker.MerchantNameRules;
public final class MerchantNameRulesTest {
    static void equal(String expected,String actual) {
        if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);
    }
    public static void main(String[] args) {
        equal("Tayba Mall",MerchantNameRules.extract("","Purchase for merchant: Tayba Mall, amount 10.00","CAPTURED"));
        equal("طيبة مول",MerchantNameRules.extract("","اشتريت لدى طيبة مول بقيمة 40 شيكل","CAPTURED"));
        equal("طيبة مول",MerchantNameRules.extract("طيبة مول","","CAPTURED"));
        equal("طيبة مول",MerchantNameRules.extract("طيبة مول","","MANUAL"));
        equal("مخبز السلام",MerchantNameRules.extract("مخبز السلام","","MANUAL"));
        equal("","".equals(MerchantNameRules.extract("طعام","","MANUAL"))?"":"unexpected");
        equal("",MerchantNameRules.extract("دفعت مصروف اليوم","","MANUAL"));
        equal("",MerchantNameRules.extract(null,null,"MANUAL"));
        equal("",MerchantNameRules.extract("","123456 123","CAPTURED"));
        equal(MerchantNameRules.key("طيبة مول"),MerchantNameRules.key("طِيبة موَل"));
        System.out.println("MerchantNameRulesTest PASS: captured merchant, Arabic shop, manual venue, unknown");
    }
}
