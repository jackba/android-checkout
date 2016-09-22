/*
 * Copyright 2014 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.checkout;

import com.android.vending.billing.IInAppBillingService;

import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class GetSkuDetailsRequest extends Request<Skus> {

    // unfortunately, Android has an undocumented limit on the size of the list in this request.
    // 20 is a number used in one of the Google samples, namely "Trivial Drive", source code of which
    // can be found here https://github.com/googlesamples/android-play-billing/blob/master/TrivialDrive/app/src/main/java/com/example/android/trivialdrivesample/util/IabHelper.java
    private static final int MAX_SIZE_PER_REQUEST = 20;

    @Nonnull
    private final String product;

    @Nonnull
    private final ArrayList<String> skus;

    GetSkuDetailsRequest(@Nonnull String product, @Nonnull List<String> skus) {
        super(RequestType.GET_SKU_DETAILS);
        this.product = product;
        this.skus = new ArrayList<>(skus);
        Collections.sort(this.skus);
    }

    @Override
    void start(@Nonnull IInAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
        final List<Sku> allSkuDetails = new ArrayList<>();
        for (int start = 0; start < skus.size(); start += MAX_SIZE_PER_REQUEST) {
            final int end = Math.min(skus.size(), start + MAX_SIZE_PER_REQUEST);
            final ArrayList<String> skuBatch = new ArrayList<>(skus.subList(start, end));
            final Skus skuDetails = getSkuDetails(service, apiVersion, packageName, skuBatch);
            if (skuDetails != null) {
                allSkuDetails.addAll(skuDetails.list);
            } else {
                // error during the request, already handled
                return;
            }
        }
        onSuccess(new Skus(product, allSkuDetails));
    }

    @Nullable
    private Skus getSkuDetails(@Nonnull IInAppBillingService service, int apiVersion, @Nonnull String packageName, ArrayList<String> skuBatch) throws RemoteException, RequestException {
        Check.isTrue(skuBatch.size() <= MAX_SIZE_PER_REQUEST, "SKU list is too big");
        final Bundle skusBundle = new Bundle();
        skusBundle.putStringArrayList("ITEM_ID_LIST", skuBatch);
        final Bundle bundle = service.getSkuDetails(Billing.V3, packageName, product, skusBundle);
        if (!handleError(bundle)) {
            return Skus.fromBundle(bundle, product);
        }
        return null;
    }

    @Nullable
    @Override
    protected String getCacheKey() {
        if (skus.size() == 1) {
            return product + "_" + skus.get(0);
        } else {
            final StringBuilder sb = new StringBuilder(5 * skus.size());
            sb.append("[");
            for (int i = 0; i < skus.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(skus.get(i));
            }
            sb.append("]");
            return product + "_" + sb.toString();
        }
    }
}
