package com.exam.order_service.dto;

public class RetryMessage<T> {
    private T data;
    private StepStatus sendEmail;
    private StepStatus updateRetryJobs;

    public RetryMessage() {}

    public RetryMessage(T data, StepStatus sendEmail, StepStatus updateRetryJobs) {
        this.data = data;
        this.sendEmail = sendEmail;
        this.updateRetryJobs = updateRetryJobs;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public StepStatus getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(StepStatus sendEmail) {
        this.sendEmail = sendEmail;
    }

    public StepStatus getUpdateRetryJobs() {
        return updateRetryJobs;
    }

    public void setUpdateRetryJobs(StepStatus updateRetryJobs) {
        this.updateRetryJobs = updateRetryJobs;
    }

    public static class StepStatus {
        private String status;
        private String message;

        public StepStatus() {}

        public StepStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
