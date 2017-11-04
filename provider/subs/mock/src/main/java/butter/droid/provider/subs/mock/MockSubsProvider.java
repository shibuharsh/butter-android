/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.provider.subs.mock;

import android.net.Uri;
import android.support.annotation.NonNull;
import butter.droid.provider.base.module.Media;
import butter.droid.provider.subs.SubsProvider;
import butter.droid.provider.subs.model.Subs;
import io.reactivex.Maybe;

public class MockSubsProvider implements SubsProvider {

    @Override public Maybe<Subs> downloadSubs(@NonNull final Media media, @NonNull final String language) {
        return Maybe.fromCallable(() -> Uri.parse("file:///android_asset/big_buck_bunny.eng.srt"))
                .map(uri -> new Subs("en", uri));
    }
}