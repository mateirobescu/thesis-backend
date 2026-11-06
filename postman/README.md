# Postman collection

One ready-to-send request per endpoint, grouped by resource.

1. In Postman: **Import** `thesis.postman_collection.json` and `local.postman_environment.json`, then pick the
   **Thesis local** environment.
2. Start the backend (`./mvnw spring-boot:run`).
3. Click through the requests. Order that works from an empty database:
   - **Auth**: *Sign up* → open the verification email in Mailpit (http://localhost:8025), copy the `token=` value
     into the collection variable `verifyToken` → *Verify email* → *Sign in*.
   - *Sign in* saves `accessToken`, `refreshToken` and `userId`; every other request is authorized with it.
   - Each *Create* request saves the new id (`workspaceId`, `projectId`, `fileId`), so the *Get / Update / Delete*
     requests below it work as soon as you click them.

The email, password and the PKCE pair used by the extension requests are collection variables you can edit.

The collection is maintained in Postman: change it there and **export it again** (Collection v2.1) over this file.
