package com.example.aesndkexample;

import android.graphics.Bitmap;

public class Item {
    String fileName;
    String rutaAbsoluta;

    public Item(String fileName, String rutaAbsoluta){
        this.fileName = fileName;
        this.rutaAbsoluta = rutaAbsoluta;
    }

    public String getRutaAbsoluta() {
        return rutaAbsoluta;
    }

    public void setRutaAbsoluta(String rutaAbsoluta) {
        this.rutaAbsoluta = rutaAbsoluta;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
