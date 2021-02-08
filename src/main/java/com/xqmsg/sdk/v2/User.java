package com.xqmsg.sdk.v2;

/**
 * The User class is used internally to store information about currently authenticated users.
 */
public class User {

    /// The first name of this user (no longer actively collected )
    public final String firstName;

    /// The last name of this user  (no longer actively collected )
    public final String lastName;

    /// The current subscription status of this user.
    public final SubscriptionStatus subscriptionStatus;

    /// This users email address.
    public final String email;

    /// This users password. This will ultimately be held in a secure location such as the keychain, and should never serialized
    /// as part of the user object as a whole.
    public final String pwd;

    /// The user constructor.
    ///
    /// - Parameters:
    ///   - email: Email address
    ///   - pwd: The users password
    ///   - firstName: The users first name
    ///   - lastName: The users last name
    ///   - status: The current subscription status of the user.
    public User(String email, String pwd, String firstName, String lastName, SubscriptionStatus status ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.subscriptionStatus = status;
        this.pwd = pwd;
    }
}