# Security

## What this project actually is

GateKeeper is an academic prototype, and the public build here is meant for local demo purposes only — it's not something you'd approve for a real building or a real access-control system.

To be specific: the demo login stores and checks sample passwords in plaintext, and the Bluetooth door commands are simple, unsigned messages — not a secure protocol. Firebase is optional and disabled by default, and even if you wire it up yourself, it's only as secure as the rules you write for it.

## Reporting a problem

If you find something concerning, please don't post exploit details, real credentials, or anything about an actual deployment in a public issue. Reach out to me privately instead (see the contact info on my GitHub profile) with the version, steps to reproduce, and what the impact would be. Leave out any personal data or live secrets.

## What a real deployment would need

At minimum, before this went anywhere near an actual door:

- A real auth provider and server-side authorization, not local plaintext passwords.
- Firebase/database rules that are actually locked down and reviewed.
- Signed, replay-resistant commands to the hardware, plus a proper device-pairing flow.
- Careful handling of credentials, device IDs, backups, and logs.
- Some actual threat modeling and testing of the physical fail-safe behavior — what happens if the app crashes, loses connection, or is compromised.
