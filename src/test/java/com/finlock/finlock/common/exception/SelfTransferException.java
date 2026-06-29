package com.finlock.finlock.common.exception;

public class SelfTransferException extends RuntimeException{
    public SelfTransferException(String message){
        super(message);
    }

}
