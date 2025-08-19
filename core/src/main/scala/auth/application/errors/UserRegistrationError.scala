package org.aulune
package auth.application.errors


/** Errors that can occur during user registration. */
enum UserRegistrationError:
  /** Chosen username is already taken. */
  case TakenUsername

  /** Username doesn't satisfy requirements. */
  case InvalidUsername

  /** User with given OAuth2 credentials already exists. */
  case OAuthUserAlreadyExists

  /** OAuth2 authorization code is invalid. */
  case InvalidOAuthCode
