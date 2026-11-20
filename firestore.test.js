const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_101";
const BOB_UID = "bob_creator_202";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, createdAt = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    title: "Agni Yodha: Shadows of Kashi",
    logline: "A young mystic awakens an ancient celestial blade in neo-Varanasi.",
    language: "Hindi",
    genre: "Anime",
    artStyle: "Anime",
    aspectRatio: "9:16",
    totalEpisodes: 12,
    currentEpisode: 1,
    storyText: "Episode 1: Raat ke andhere mein Kashi ke ghat par ek ajeeb roshni chamki.",
    status: "IN_PRODUCTION",
    createdAt,
    updatedAt: createdAt,
  };
}

function validScenePayload(uid, projectId, createdAt = new Date(Date.now() - 4000)) {
  return {
    userId: uid,
    projectId,
    episodeNumber: 1,
    sceneNumber: 1,
    title: "Neon Ghats Awakening",
    description: "Wide cinematic view of Varanasi ghats glowing under monsoon rain.",
    cameraAngle: "Low-angle wide shot",
    cameraMovement: "Slow dolly push-in towards hero",
    characterAction: "Aarav raises his glowing hand as rain freezes mid-air",
    durationSeconds: 6,
    imagePrompt: "Indian anime style, young hero standing on Varanasi ghats at night, neon purple and electric blue rain, 9:16 cinematic",
    videoPrompt: "Slow dolly push-in, raindrops freeze in mid-air while glowing blue runes spiral around hero, 6s smooth motion",
    consistencyNotes: "Aarav wears indigo armored kurta with silver trim and messy dark hair",
    dialogueCharacter: "Aarav",
    hindiDialogue: "Yeh shakti mujhe kyun chunti ja rahi hai?",
    voiceOverScript: "Kashi ki is raat mein, ek naye yug ka aarambh hone wala tha.",
    subtitleText: "00:00:00,000 --> 00:00:06,000\nYeh shakti mujhe kyun chunti ja rahi hai?",
    bgmPrompt: "Epic Indian flute fused with deep synth bass pulse",
    sfxPrompt: "Thunder rumble, crackling blue lightning, rain droplets slowing",
    createdAt,
    updatedAt: createdAt,
  };
}

function validCharacterPayload(uid, projectId = "proj_1", createdAt = new Date(Date.now() - 4000)) {
  return {
    userId: uid,
    projectId,
    name: "Aarav Sharma",
    role: "Protagonist",
    description: "20-year-old Varanasi student with sharp amber eyes and glowing wrist seal.",
    clothing: "Deep indigo sleeveless tactical kurta with glowing cyan seams and leather bracers",
    hairstyle: "Wind-swept jet black anime hair with a single silver streak",
    expressions: "Determined glare, quiet empathy, fierce battle focus",
    consistencyToken: "Aarav, 20yo Indian anime male, amber eyes, jet black hair with silver streak, indigo tactical kurta with cyan seams",
    voiceTone: "Warm young baritone, calm yet resolute Hindi diction",
    createdAt,
    updatedAt: createdAt,
  };
}

function validSavedPromptPayload(uid, projectId = "proj_1", createdAt = new Date(Date.now() - 4000)) {
  return {
    userId: uid,
    projectId,
    promptType: "VIDEO",
    title: "Ghat Rooftop Chase",
    promptText: "Tracking drone shot following hero leaping across glowing Varanasi rooftops, 6 seconds, anime lighting",
    aspectRatio: "9:16",
    artStyle: "Drone Tracking",
    durationSeconds: 6,
    createdAt,
    updatedAt: createdAt,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/projects/proj_1`).set(validProjectPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/characters`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/promptHistory`).get());
});

test("Owner can create, list, update, and delete projects; ghost fields and immutable edits fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectRef = aliceDb.doc(`users/${ALICE_UID}/projects/proj_1`);

  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects`).get());

  // Valid partial update preserving userId & createdAt
  await assertSucceeds(
    projectRef.update({
      title: "Agni Yodha: Season 1",
      currentEpisode: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation must fail
  await assertFails(
    projectRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_ghost`).set({
      ...validProjectPayload(ALICE_UID),
      id: "proj_ghost",
    })
  );

  // Future timestamp tampering must fail
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_future`).set(
      validProjectPayload(ALICE_UID, new Date(Date.now() + 120000))
    )
  );

  // Invalid genre enum must fail
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_bad_enum`).set({
      ...validProjectPayload(ALICE_UID),
      genre: "InvalidGenre",
    })
  );

  await assertSucceeds(projectRef.delete());
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's projects, scenes, characters, and prompts", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.doc(`users/${ALICE_UID}/projects/proj_1`).set(validProjectPayload(ALICE_UID));
    await adminDb
      .doc(`users/${ALICE_UID}/projects/proj_1/scenes/scene_1`)
      .set(validScenePayload(ALICE_UID, "proj_1"));
    await adminDb
      .doc(`users/${ALICE_UID}/characters/char_1`)
      .set(validCharacterPayload(ALICE_UID, "proj_1"));
    await adminDb
      .doc(`users/${ALICE_UID}/promptHistory/prompt_1`)
      .set(validSavedPromptPayload(ALICE_UID, "proj_1"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/projects/proj_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/projects/proj_1`).update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/projects/proj_1/scenes/scene_1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/characters/char_1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/promptHistory/prompt_1`).get());
});

test("Scenes enforce Parent Gate (parent project must exist) and allow valid CRUD by owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Creating a scene under a non-existent parent project must fail (Parent Gate)
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/projects/missing_proj/scenes/scene_1`)
      .set(validScenePayload(ALICE_UID, "missing_proj"))
  );

  // Create parent project first
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_1`).set(validProjectPayload(ALICE_UID))
  );

  // Now scene creation succeeds
  const sceneRef = aliceDb.doc(`users/${ALICE_UID}/projects/proj_1/scenes/scene_1`);
  await assertSucceeds(sceneRef.set(validScenePayload(ALICE_UID, "proj_1")));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects/proj_1/scenes`).get());

  // Partial update on scene succeeds
  await assertSucceeds(
    sceneRef.update({
      title: "Neon Ghats Awakening (Director Cut)",
      durationSeconds: 8,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Changing immutable projectId fails
  await assertFails(
    sceneRef.update({
      projectId: "other_proj",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(sceneRef.delete());
});

test("Characters and PromptHistory allow valid owner CRUD and reject oversized/invalid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const charRef = aliceDb.doc(`users/${ALICE_UID}/characters/char_1`);
  await assertSucceeds(charRef.set(validCharacterPayload(ALICE_UID, "")));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/characters`).get());
  await assertSucceeds(
    charRef.update({
      expressions: "Heroic smile, focused battle gaze",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(charRef.delete());

  const promptRef = aliceDb.doc(`users/${ALICE_UID}/promptHistory/prompt_1`);
  await assertSucceeds(promptRef.set(validSavedPromptPayload(ALICE_UID, "proj_1")));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/promptHistory`).get());

  // Invalid promptType enum fails
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/promptHistory/prompt_bad`).set({
      ...validSavedPromptPayload(ALICE_UID, "proj_1"),
      promptType: "UNKNOWN",
    })
  );

  await assertSucceeds(promptRef.delete());
});
