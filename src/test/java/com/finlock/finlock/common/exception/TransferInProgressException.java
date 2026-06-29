package com.finlock.finlock.common.exception;

public class TransferInProgressException extends RuntimeException {
   public TransferInProgressException(String message) {
      super(message);
   }
}
