package core.apis.youtube;

import org.junit.Test;

import static org.junit.Assert.*;

public class InvidousSearchTest {

    @Test
    public void doSearch() {
        InvidousSearch invidousSearch = new InvidousSearch();
        String s = invidousSearch.doSearch("counterparts you are not you anymore");
    }
}