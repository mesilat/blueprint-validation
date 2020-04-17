package com.mesilat.vbp.api;

import java.util.List;

public interface ValidatorManager {
    List<Validator> list(boolean details);
    Validator get(String code);
    void delete(String code);
    void create(Validator validator);
    void create(List<Validator> validators);
    void update(String code, Validator validator);
    String css();
}