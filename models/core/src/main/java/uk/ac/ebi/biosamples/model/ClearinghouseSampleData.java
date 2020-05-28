package uk.ac.ebi.biosamples.model;

public class ClearinghouseSampleData {
    private String total;
    private ClearinghouseCurations[] curations;

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public ClearinghouseCurations[] getCurations() {
        return curations;
    }

    public void setCurations(ClearinghouseCurations[] curations) {
        this.curations = curations;
    }

    @Override
    public String toString() {
        return "ClearinghouseSampleData [total = " + total + ", curations = " + curations + "]";
    }
}

