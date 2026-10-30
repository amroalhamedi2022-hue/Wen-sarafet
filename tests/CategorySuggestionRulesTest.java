import app.wensarafet.tracker.CategorySuggestionRules;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CategorySuggestionRulesTest {
    public static void main(String[] args) {
        List<CategorySuggestionRules.Example> known=Arrays.asList(
           x(2,1,200,"مشوار",""),x(2,1,200,"تاكسي",""),x(2,1,200,"مواصلات", ""));
        eq(2,CategorySuggestionRules.suggest(1,200,"","",known),"2 ILS repeat => transport");
        if(CategorySuggestionRules.suggest(3,200,"","",known)!=null)throw new AssertionError("account leakage");
        if(CategorySuggestionRules.suggest(1,250,"","",known)!=null)throw new AssertionError("amount leakage");
        List<CategorySuggestionRules.Example> mixed=Arrays.asList(x(2,1,200,"",""),x(2,1,200,"",""),
          x(5,1,200,"",""),x(5,1,200,"",""));
        if(CategorySuggestionRules.suggest(1,200,"","",mixed)!=null)throw new AssertionError("ambiguous amount");
        List<CategorySuggestionRules.Example> shop=Arrays.asList(x(3,1,500,"مخبز الهدى",""));
        eq(3,CategorySuggestionRules.suggest(1,700,"مخبز ألهُدى","",shop),"description normalization");
        eq(3,CategorySuggestionRules.suggest(4,700,"مخبز الهدى","",shop),"description despite account");
        if(CategorySuggestionRules.suggest(1,200,"","",Collections.emptyList())!=null)
            throw new AssertionError("empty history");
        // An amount repeated at one account must not defeat conflicting explicit shop history.
        List<CategorySuggestionRules.Example> conflicting=Arrays.asList(
            x(2,1,200,"تاكسي",""),x(2,1,200,"سيارة",""),x(2,1,200,"حافلة",""),
            x(5,1,300,"سوبرماركت",""),x(7,1,400,"سوبرماركت",""));
        if(CategorySuggestionRules.suggest(1,200,"سوبرماركت","",conflicting)!=null)
            throw new AssertionError("ambiguous merchant overridden by amount frequency");
        // Invalid old records must not train the suggestion engine.
        List<CategorySuggestionRules.Example> invalid=Arrays.asList(
            x(9,1,-100,"صيدلية",""),x(9,1,0,"صيدلية",""));
        if(CategorySuggestionRules.suggest(1,200,"صيدلية","",invalid)!=null)
            throw new AssertionError("invalid history trained a category");
        System.out.println("CategorySuggestionRulesTest: all checks passed");
    }
    static CategorySuggestionRules.Example x(long category,long account,long amount,String note,String merchant){
        return new CategorySuggestionRules.Example(category,account,amount,note,merchant);
    }
    static void eq(long expected,CategorySuggestionRules.Suggestion actual,String msg){
        if(actual==null || actual.categoryId!=expected)throw new AssertionError(msg);
    }
}
