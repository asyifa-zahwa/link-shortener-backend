package com.example.linkshortener.dto;

public interface ClickCountTuple {
    String getKey();   // Akan menampung nama kategori (misal: 'chrome' atau 'mobile')
    Long getCount();   // Akan menampung angka jumlah kliknya
}