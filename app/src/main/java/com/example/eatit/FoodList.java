package com.example.eatit;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.eatit.Interface.itemClickListner;
import com.example.eatit.Model.Food;
import com.example.eatit.ViewHolder.FoodViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class FoodList extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;

    String categoryId = "";

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");

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
            loadFoodList(categoryId);
        }


    }

    private void loadFoodList(String categoryId) {
     adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class,
             R.layout.food_item,
             FoodViewHolder.class,
             foodList.orderByChild("menuId").equalTo(categoryId)) // Select * fron foods where menuid = categoryid
     {
         @Override
         protected void populateViewHolder(FoodViewHolder viewHolder, Food model, int position) {
             viewHolder.food_name.setText(model.getName());
             Picasso.with(getBaseContext()).load(model.getImage())
                     .into(viewHolder.food_image);

             final Food local = model;
             viewHolder.setItemClickListner(new itemClickListner() {
                 @Override
                 public void onClick(View view, int position, boolean isLongClick) {
                     Toast.makeText(FoodList.this, ""+local.getName(), Toast.LENGTH_SHORT).show();
                 }
             });
         }
     };

     //Set Adapter
        recyclerView.setAdapter(adapter);
    }
}