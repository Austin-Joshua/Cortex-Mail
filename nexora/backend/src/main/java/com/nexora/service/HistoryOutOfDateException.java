package com.nexora.service;

/** Stored Gmail historyId is invalid or expired — a full sync is required. */
class HistoryOutOfDateException extends Exception {
    HistoryOutOfDateException(String message) {
        super(message);
    }
}
