package com.xqmsg.sdk.v2;

public enum SubscriptionStatus {

        ///  An invalid, unregistered user.
        Unregistered(0),
        /// A valid but unsubscribed user.
        NotSubscribed(1),
        /// A currently subscribed user.
        Subscribed(2),
        /// An expired account.
        Expired(3),
        /// A new account that still needs to be validated.
        NewAccountValidationRequired(4),
        /// The user is valid but needs to set a new password.
        PasswordRequired(5),
        /// The user has an existing account needing revalidation.
        OldAccountValidationRequired(6);


        private final int id;

        SubscriptionStatus(int id) {
            this.id = id;
        }

        public int getCode() {
            return id;
        }

        public static SubscriptionStatus valueOf(Integer code) {
            switch (code) {
                case 1: return NotSubscribed;
                case 2: return Subscribed;
                case 3: return Expired;
                case 4: return NewAccountValidationRequired;
                case 5: return PasswordRequired;
                case 6: return OldAccountValidationRequired;
                default: return Unregistered;
            }
        }

    }
