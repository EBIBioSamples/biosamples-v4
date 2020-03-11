package uk.ac.ebi.biosamples;

import uk.ac.ebi.biosamples.utils.ThreadUtils;

public class PipelineFutureCallback implements ThreadUtils.Callback<Integer> {
    private long totalCount = 0;

    public void call(Integer count) {
        totalCount = totalCount + count;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
