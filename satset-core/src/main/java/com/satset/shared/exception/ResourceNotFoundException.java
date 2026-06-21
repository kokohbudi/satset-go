package com.satset.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + " not found: " + resourceId);
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
