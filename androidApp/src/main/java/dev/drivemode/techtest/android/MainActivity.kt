@file:OptIn(ExperimentalMaterial3Api::class)

package dev.drivemode.techtest.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import dev.drivemode.techtest.BookModel
import dev.drivemode.techtest.DetailModel
import dev.drivemode.techtest.DetailViewModel
import dev.drivemode.techtest.SearchViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            SearchViewModel(),
                            navController,
                        )
                    }
                    composable("detail/{key}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("key").orEmpty()
                        DetailScreen(id, DetailViewModel())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    searchViewModel: SearchViewModel,
    navController: NavController,
) {
    var isLoading by remember { mutableStateOf(false) }
    val bookList = remember { mutableStateListOf<BookModel>() }
    var token by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val searchAndUpdateBooks =
        remember(searchViewModel) {
            {
                scope.launch {
                    isLoading = true
                    bookList.clear()
                    val works = searchViewModel.searchBySubject(token)
                    bookList.addAll(works.books)
                    isLoading = false
                    snackbarHostState.showSnackbar("Loaded at ${works.timeStamp}")
                }
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                query = token,
                onQueryChange = { token = it },
                onSearch = { searchAndUpdateBooks() },
                placeholder = { Text("Enter your search query") },
                leadingIcon = {
                    Icon(
                        modifier =
                            Modifier.clickable {
                                searchAndUpdateBooks()
                            },
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                active = false,
                onActiveChange = {},
            ) {}

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(all = 8.dp),
                    ) {
                        bookList.forEach { book ->
                            item {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 4.dp)
                                            .clickable {
                                                navController.navigate("detail/${book.key}")
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Image(
                                        modifier =
                                            Modifier
                                                .size(150.dp)
                                                .padding(end = 8.dp),
                                        contentScale = ContentScale.Crop,
                                        painter = rememberImagePainter(data = book.imageUrl),
                                        contentDescription = "",
                                    )
                                    Text(
                                        text = book.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DetailScreen(
    key: String,
    detailViewModel: DetailViewModel,
) {
    var isLoading by remember { mutableStateOf(true) }
    var detailModel by remember { mutableStateOf(DetailModel.EMPTY) }

    LaunchedEffect(key1 = detailViewModel) {
        detailModel = detailViewModel.getDetails(key)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                Image(
                    painter = rememberImagePainter(data = detailModel.book.imageUrl),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(16.dp),
                )
                Text(
                    text = detailModel.book.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = detailModel.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
