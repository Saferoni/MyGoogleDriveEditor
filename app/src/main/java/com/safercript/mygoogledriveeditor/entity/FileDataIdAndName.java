package com.safercript.mygoogledriveeditor.entity;

public class FileDataIdAndName {
    private String id;
    private String nameFile;

    public FileDataIdAndName(String id, String nameFile) {
        this.id = id;
        this.nameFile = nameFile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameFile() {
        return nameFile;
    }

    public void setNameFile(String nameFile) {
        this.nameFile = nameFile;
    }
}
