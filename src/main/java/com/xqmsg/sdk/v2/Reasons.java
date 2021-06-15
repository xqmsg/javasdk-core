package com.xqmsg.sdk.v2;



/**
 * Created by ikechie on 2/3/20.
 */

public enum Reasons {
    /// Parameters were missing from the initial call.
    MissingParameters,
    /// Usually indicates that invalid data was sent to the server.
    InvalidPayload,
    /// The current user does not have the permissions to perform the requested action.
    InvalidUser,
    /// An invalid quantum key was provided.
    InvalidQuantumKey,
    /// An invalid encryption algorithm was specified.
    InvalidAlgorithm,
    /// An encryption attempt failed.
    EncryptionFailed,
    /// A decryption attempt failed.
    DecryptionFailed,
    /// An invalid encryption key was specified.
    InvalidEncryptionKey,
    /// An invalid token was specified.
    InvalidToken,
    /// Requested action has not yet been implemented.
    NotImplemented,
    ////
    InternalException,

    LocalException,
    ////
    SourceFileNotFound,

    FileCreateFailed,

    OTPKeyLengthIncorrect,

    AESKeyLengthIncorrect,

    OutputFileCreationFailed,

    FileNotFound,

    IOException,

    Unauthorized,

    MissingEncryptionKey,

    NoneProvided
}