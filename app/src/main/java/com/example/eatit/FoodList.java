package com.example.eatit;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.eatit.Common.Common;
import com.example.eatit.Database.Database;
import com.example.eatit.Interface.itemClickListner;
import com.example.eatit.Model.Food;
import com.example.eatit.ViewHolder.FoodViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FoodList extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;

    String categoryId = "";

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    //Search Functionality
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    //Favourites
    Database localDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");

        //Local db
        localDb = new Database(this);

        recyclerView = findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Get intent
        if(getIntent() != null)
        {
            categoryId = getIntent().getStringExtra("CategoryId");
        }
        if(!categoryId.isEmpty() && categoryId != null)
        {
            if(Common.isConnectedtoInternet(getBaseContext()))
                loadFoodList(categoryId);
            else
            {
                Toast.makeText(FoodList.this,"Please check your connection",Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Search
        materialSearchBar = findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter your food");
        loadSuggestions();
        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //change suggestions list when user types

                List<String> suggest = new ArrayList<String>();
                for(String search:suggestList)
                {
                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                //Restore original adapter when search bard is closed
                if(!enabled)
                    recyclerView.setAdapter(adapter);

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //show result of search when search finishes
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });
    }

    private void startSearch(CharSequence text) {
        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList.orderByChild("name").equalTo(text.toString())
        ) {
            @Override
            protected void populateViewHolder(FoodViewHolder viewHolder, Food model, int position) {
                viewHolder.food_name.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(viewHolder.food_image);

                final Food local = model;
                viewHolder.setItemClickListner(new itemClickListner() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Start new activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",searchAdapter.getRef(position).getKey());
                        startActivity(foodDetail);
                    }
                });
            }

        };
        recyclerView.setAdapter(searchAdapter);
    }

    private void loadSuggestions() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot:dataSnapshot.getChildren())
                        {
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadFoodList(String categoryId) {
     adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class,
             R.layout.food_item,
             FoodViewHolder.class,
             foodList.orderByChild("menuId").equalTo(categoryId)) // Select * fron foods where menuid = categoryid
     {
         @Override
         protected void populateViewHolder(final FoodViewHolder viewHolder, final Food model, final int position) {
             final ColorStateList csl = AppCompatResources.getColorStateList(getBaseContext(), R.color.colorRed);
             viewHolder.food_name.setText(model.getName());
             Picasso.with(getBaseContext()).load(model.getImage())
                     .into(viewHolder.food_image);

             //Add Favourites
             if(localDb.isFavourite(adapter.getRef(position).getKey()))
                 viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
             //change state of Favorite
             viewHolder.fav_image.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     if(!localDb.isFavourite(adapter.getRef(position).getKey()))
                     {
                         localDb.addToFavourites(adapter.getRef(position).getKey());
                         viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                         ImageViewCompat.setImageTintList(viewHolder.fav_image, csl);
                         Toast.makeText(FoodList.this,""+model.getName()+" was added to Favourites",Toast.LENGTH_SHORT);
                     }
                     else
                     {
                         localDb.removeFromFavourites(adapter.getRef(position).getKey());
                         viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                         Toast.makeText(FoodList.this,""+model.getName()+" was removed from Favourites",Toast.LENGTH_SHORT);
                     }
                 }
             });


             final Food local = model;
             viewHolder.setItemClickListner(new itemClickListner() {
                 @Override
                 public void onClick(View view, int position, boolean isLongClick) {
                     //Start new activity
                     Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                     foodDetail.putExtra("FoodId",adapter.getRef(position).getKey());
                     startActivity(foodDetail);
                 }
             });
         }
     };

     //Set Adapter
        recyclerView.setAdapter(adapter);
    }
}
