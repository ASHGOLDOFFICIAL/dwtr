# dwtr

---

## About

**dwtr** is an API for managing translations of various media — including
prose, comics, and audio plays. The goal is to keep track of what has
already been translated in a simple and accessible way.

> **Note**: Currently only audio play are supported.

You can create people (creators, translators, voice actors, etc.),
series, audio plays and their translations.

Service does *not* provide a way to host content. It only stores metadata
about people, series, audio plays, and their translations.

---

## Permissions

Permission are used to restrict some action to specific users. Here's
an overview of every one of them:

| Permission                 | Description                                                                                                                       |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| aggregator.modify          | Allows to modify content and people inside aggregator module.                                                                     |
| aggregator.see_self_hosted | Grants access to view self-hosted content links (e.g., Jellyfin, Google Drive, etc.) that are normally hidden from regular users. |

There's also an admin permission that's determined based on `ADMIN_PERMISSION_NAMESPACE`
and `ADMIN_PERMISSION_NAME` environment variables. User with admin permission can
perform any restricted action.

---

## Deployment

To run the API locally:

1. **Set up Google OAuth Client**  
   You can follow [this guide](https://www.youtube.com/watch?v=TjMhPr59qn4). Only the `openid` scope is required.
   > Currently, it cannot be disabled, but in theory, respective environment variables
   > can be filled with dummy values to just make application work.
2. **Prepare environment variables**  
   Copy `.env.template` to `.env`, and fill in the required values.
3. **Start the service**  
   Run the following command:
   ```bash
     docker compose up
   ```
4. **Create an admin user**  
   Use [this script](./scripts/users/README.md).
5. **Browse the API**  
   Visit `/v1/docs` to view the API documentation via Swagger UI.

---

## Tech Stack

Application is written in Scala 3, tagless final style, PostgreSQL,
MinIO, Docker.

`cats-effect` is used for effects.
`tapir` is used for HTTP-endpoints.
`doobie` is used for communication with database.
`circe` is used for JSON.

---

## Contributing

Licence is to be decided. If you discovered a bug,
report it on GitHub (preferably in English, just to be formal).
Feature suggestion are also welcome.