package com.mjc813.currentstreambackend.models.common;

/*
    수신 오류 코드
 */
public enum ResponseCode {
    insert_ok,
    update_ok,
    delete_ok,
    select_ok,
    insert_failed,
    update_failed,
    delete_failed,
    select_failed,
    failed
}
