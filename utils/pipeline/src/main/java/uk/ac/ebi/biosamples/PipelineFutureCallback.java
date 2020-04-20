package uk.ac.ebi.biosamples;

import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

public class PipelineFutureCallback implements ThreadUtils.Callback<PipelineResult> {
    private long totalCount = 0;
    private final List<String> failedSamples = new ArrayList<>();

    public void call(PipelineResult pipelineResult) {
        totalCount = totalCount + pipelineResult.getModifiedRecords();
        if (!pipelineResult.isSuccess()) {
            failedSamples.add(pipelineResult.getAccession());
        }
    }

    public long getTotalCount() {
        return totalCount;
    }

    public List<String> getFailedSamples() {
        return failedSamples;
    }
}
