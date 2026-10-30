package no.nav.ereg.integrasjon.dolly;

public enum JobbKjøringStatus {
    PENDING, SUCCEEDED, RUNNING, FAILED, UNKNOWN;

    public boolean kanVæreIGang() {
        return switch(this){
            case PENDING -> true;
            case SUCCEEDED -> false;
            case RUNNING -> true;
            case FAILED -> false;
            case UNKNOWN -> true;
        };
    }
}
