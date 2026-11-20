"""Pure/authentication tests of legacy free-coin ownership as an additive veto."""
import importlib
import unittest
from unittest.mock import MagicMock, patch

from legacy_ownership import (
    _record_impl, legacy_registered, read_veto_snapshot, validate_legacy_items,
)
from wallet_rules import WalletError


class LegacyOwnershipTests(unittest.TestCase):
    def test_whitelisted_item_ids_and_no_duplicates(self):
        self.assertEqual(validate_legacy_items(["skin_egypt", "theme_ocean"]),
                         ["skin_egypt", "theme_ocean"])
        for ids in (
            ["skin_fake"], ["skin_egypt", "skin_egypt"],
            ["skin_green"], None, {}, "skin_egypt",
            [True], ["skin_egypt", 7],
        ):
            with self.subTest(ids=ids), self.assertRaises(WalletError):
                validate_legacy_items(ids)

    def test_monotonic_merge_never_erases_previously_reported_items(self):
        tx, ref = MagicMock(), MagicMock()
        ref.get.return_value.exists = True
        ref.get.return_value.to_dict.return_value = {
            "schemaVersion": 1, "ownedItemIds": ["skin_egypt"]
        }
        result = _record_impl(tx, ref, ["theme_ocean"])
        self.assertEqual(result, {"legacyOwnershipReady": True, "blockedPaidItemCount": 2})
        written = tx.set.call_args.args[1]
        self.assertEqual(written["ownedItemIds"], ["skin_egypt", "theme_ocean"])

        tx.reset_mock()
        result = _record_impl(tx, ref, [])
        self.assertEqual(result["blockedPaidItemCount"], 1)
        tx.set.assert_not_called()

    def test_missing_or_corrupt_snapshot_not_ready(self):
        snap = MagicMock()
        snap.exists = False
        with self.assertRaisesRegex(WalletError, "legacy_sync_required"):
            read_veto_snapshot(snap)
        snap.exists = True
        snap.to_dict.return_value = {"schemaVersion": 3, "ownedItemIds": []}
        with self.assertRaises(WalletError):
            read_veto_snapshot(snap)

    def test_registered_checks_server_document(self):
        db = MagicMock()
        snap = db.collection.return_value.document.return_value.get.return_value
        snap.exists = False
        self.assertFalse(legacy_registered(db, "player"))
        snap.exists = True
        snap.to_dict.return_value = {"schemaVersion": 1, "ownedItemIds": []}
        self.assertTrue(legacy_registered(db, "player"))


class LegacyOwnershipRouteTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with patch("google.cloud.firestore.Client", return_value=MagicMock()):
            cls.server = importlib.import_module("main")
        cls.client = cls.server.app.test_client()

    def test_requires_session_and_whitelisted_payload(self):
        with patch.object(self.server, "session_player", return_value=None):
            result = self.client.post("/legacy-ownership", json={"ownedItemIds": []})
        self.assertEqual(result.status_code, 401)

        with (patch.object(self.server, "session_player", return_value="player"),
              patch.object(self.server, "register_legacy_veto") as register):
            for payload in ({"other": []},
                            {"ownedItemIds": [], "playerId": "fake"}):
                result = self.client.post("/legacy-ownership", json=payload)
                self.assertEqual(result.status_code, 400)
            register.assert_not_called()

    def test_authenticated_report_only_registers_veto(self):
        with (patch.object(self.server, "session_player", return_value="player"),
              patch.object(self.server, "register_legacy_veto",
                           return_value={"legacyOwnershipReady": True,
                                         "blockedPaidItemCount": 1}) as register):
            response = self.client.post("/legacy-ownership",
                                        json={"ownedItemIds": ["skin_egypt"]})
        self.assertEqual(response.status_code, 200)
        register.assert_called_once_with(self.server.db, "player", ["skin_egypt"])
        self.assertEqual(response.get_json()["blockedPaidItemCount"], 1)
        self.assertNotIn("paidBalance", response.get_json())


if __name__ == "__main__":
    unittest.main()
