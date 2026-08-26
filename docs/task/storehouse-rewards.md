# Storehouse Rewards

The Daily sidebar is the stable entry point for both Storehouse rewards. `Online Rewards`
centers the Storehouse independently of the previous city camera position. Its first frame may
show a pointing hand over the chest bubble; selecting the visible building body and pressing
Android Back removes that selection overlay and exposes the normal chest template. The routine
must verify the chest after Back instead of using the hand as an interaction target.

`A Warm Welcome` opens the stamina popup directly. Claiming requires the detected green Claim
control; arbitrary popup taps are unsafe because the popup treats other taps as exit actions.
The displayed reward amount is read before the claim when possible, and stamina accounting is
updated only after the Claim control disappears.

Saved redacted evidence is under `modules/tasks/src/test/resources/live-regressions-20260826`.
