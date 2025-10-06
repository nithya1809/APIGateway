package com.example.demo.dto;

import java.util.List;

/**
 * Data Transfer Object for user details.
 * Contains username, password, and roles assigned to the user.
 */
public class UserDTO {
    private String username;
    private String password;
    private List<String> roles;

    /**
     * Default no-argument constructor.
     */
    public UserDTO() {}

    /**
     * Gets the username of the user.
     *
     * @return the username as a String
     */
    public String getUsername() { return username; }

    /**
     * Sets the username of the user.
     *
     * @param username the username to set
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Gets the password of the user.
     *
     * @return the password as a String
     */
    public String getPassword() { return password; }

    /**
     * Sets the password of the user.
     *
     * @param password the password to set
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Gets the roles assigned to the user.
     *
     * @return a list of roles as Strings
     */
    public List<String> getRoles() { return roles; }

    /**
     * Sets the roles assigned to the user.
     *
     * @param roles the list of roles to set
     */
    public void setRoles(List<String> roles) { this.roles = roles; }
}
