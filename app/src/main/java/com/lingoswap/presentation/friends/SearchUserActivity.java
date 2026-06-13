package com.lingoswap.presentation.friends;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.lingoswap.databinding.ActivitySearchUserBinding;
import com.lingoswap.data.model.SearchUserResponse;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SearchUserActivity extends AppCompatActivity {

    private ActivitySearchUserBinding binding;
    private FriendsViewModel          viewModel;
    private SearchUserAdapter         adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        // FIX: adapter nhận SearchUserResponse.SearchUser
        adapter = new SearchUserAdapter(user -> viewModel.sendFriendRequest(user.getId()));
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(adapter);

        // Tìm khi nhấn Enter
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // Tìm realtime khi gõ >= 2 ký tự
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (s.length() >= 2) {
                    viewModel.searchNewUsers(s.toString());
                } else if (s.length() == 0) {
                    adapter.setUsers(null);
                    binding.tvNoResults.setVisibility(View.GONE);
                }
            }
        });
    }

    private void performSearch() {
        String query = binding.etSearch.getText().toString().trim();
        if (!query.isEmpty()) viewModel.searchNewUsers(query);
    }

    private void observeViewModel() {
        // FIX: observe List<SearchUserResponse.SearchUser>
        viewModel.searchResults.observe(this, users -> {
            adapter.setUsers(users);
            boolean empty = users == null || users.isEmpty();
            binding.tvNoResults.setVisibility(
                    empty && binding.etSearch.getText().length() >= 2
                            ? View.VISIBLE : View.GONE);
        });

        viewModel.isLoading.observe(this, loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.successMessage.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                // Disable nút kết bạn của item vừa gửi (refresh adapter)
                adapter.notifyDataSetChanged();
            }
        });
    }
}
