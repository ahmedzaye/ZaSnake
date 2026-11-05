import copy
import unittest
from purchase_validation import validate_product_purchase_v2, PurchaseValidationError

PRODUCT = "zasnake_coins_1000"
ACCOUNT = "0123456789abcdef" * 4


def valid_receipt():
    return {
        "purchaseStateContext": {"purchaseState": "PURCHASED"},
        "obfuscatedExternalAccountId": ACCOUNT,
        "productLineItem": [{
            "productId": PRODUCT,
            "productOfferDetails": {"quantity": 1, "refundableQuantity": 1,
                                    "consumptionState": "CONSUMPTION_STATE_YET_TO_BE_CONSUMED"},
        }],
    }


class ValidationTests(unittest.TestCase):
    def check_rejected(self, modify):
        r = valid_receipt()
        modify(r)
        with self.assertRaises(PurchaseValidationError):
            validate_product_purchase_v2(r, PRODUCT, ACCOUNT)

    def test_valid(self):
        self.assertEqual(validate_product_purchase_v2(valid_receipt(), PRODUCT, ACCOUNT),
                         "CONSUMPTION_STATE_YET_TO_BE_CONSUMED")

    def test_already_consumed_resumes(self):
        r = valid_receipt()
        r["productLineItem"][0]["productOfferDetails"]["consumptionState"] = "CONSUMPTION_STATE_CONSUMED"
        self.assertEqual(validate_product_purchase_v2(r, PRODUCT, ACCOUNT), "CONSUMPTION_STATE_CONSUMED")

    def test_pending_rejected(self):
        self.check_rejected(lambda r: r["purchaseStateContext"].update(purchaseState="PENDING"))

    def test_cancelled_rejected(self):
        self.check_rejected(lambda r: r["purchaseStateContext"].update(purchaseState="CANCELLED"))

    def test_other_user_rejected(self):
        self.check_rejected(lambda r: r.update(obfuscatedExternalAccountId="0" * 64))

    def test_missing_account_rejected(self):
        self.check_rejected(lambda r: r.pop("obfuscatedExternalAccountId"))

    def test_other_product_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0].update(productId="fake"))

    def test_two_items_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"].append(copy.deepcopy(r["productLineItem"][0])))

    def test_multi_quantity_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0]["productOfferDetails"].update(quantity=2))

    def test_missing_quantity_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0]["productOfferDetails"].pop("quantity"))

    def test_refund_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0]["productOfferDetails"].update(refundableQuantity=0))

    def test_unknown_consumption_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0]["productOfferDetails"].update(consumptionState="UNKNOWN"))

    def test_rent_offer_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0]["productOfferDetails"].update(rentOfferDetails={"rentalPeriod":"48h"}))

    def test_preorder_rejected(self):
        self.check_rejected(lambda r: r["productLineItem"][0]["productOfferDetails"].update(preorderOfferDetails={"preorderReleaseTime":"2027-01-01T00:00:00Z"}))

    def test_invalid_payload_rejected(self):
        with self.assertRaises(PurchaseValidationError):
            validate_product_purchase_v2(None, PRODUCT, ACCOUNT)


if __name__ == "__main__":
    unittest.main()
