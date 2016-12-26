package ru.ganev.plugins;

/**
 *
 */
public enum ScmEnum {

    GIT("git"),
    HG("hg");

    private String value;

    ScmEnum(String name) {
        this.value = name;
    }

    /**
     * @return
     */
    public String value() {
        return value;
    }

}
