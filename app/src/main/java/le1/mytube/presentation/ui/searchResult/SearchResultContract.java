package le1.mytube.presentation.ui.searchResult;

import java.util.List;

import le1.mytube.data.database.youTubeSong.YouTubeSong;
import le1.mytube.presentation.ui.base.BaseContract;

interface SearchResultContract extends BaseContract {

    interface View extends BaseContract.View {

        void onStartLoading();
        void onStopLoading();

        void onSearchResultLoaded(List<YouTubeSong> songList);
        void onNoSearchResultLoaded();
        void onSearchResultError(String message);


    }

    interface ViewModel extends BaseContract.ViewModel {
        @Override
        void setContractView(BaseContract.View contractView);

        void downloadSong(YouTubeSong youTubeSong);

        void addSongToQueue(YouTubeSong youTubeSong);

        void loadSearchResult(String query);
    }
}
