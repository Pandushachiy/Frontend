package com.health.companion;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.health.companion.data.local.dao.ChatMessageDao;
import com.health.companion.data.local.dao.ConversationDao;
import com.health.companion.data.local.dao.DocumentDao;
import com.health.companion.data.local.dao.HealthMetricDao;
import com.health.companion.data.local.dao.MoodEntryDao;
import com.health.companion.data.local.database.HealthCompanionDatabase;
import com.health.companion.data.remote.TokenAuthenticator;
import com.health.companion.data.remote.api.AuthApi;
import com.health.companion.data.remote.api.ChatApi;
import com.health.companion.data.remote.api.DashboardApi;
import com.health.companion.data.remote.api.DocumentApi;
import com.health.companion.data.remote.api.HealthApi;
import com.health.companion.data.remote.api.ProfileApi;
import com.health.companion.data.remote.api.VoiceApi;
import com.health.companion.data.repositories.AuthRepository;
import com.health.companion.data.repositories.ChatRepository;
import com.health.companion.data.repositories.DashboardRepository;
import com.health.companion.data.repositories.DocumentRepository;
import com.health.companion.data.repositories.HealthRepository;
import com.health.companion.data.repositories.ProfileRepository;
import com.health.companion.data.repositories.VoiceRepository;
import com.health.companion.di.AppModule_ProvidePreferencesDataStoreFactory;
import com.health.companion.di.DatabaseModule_ProvideChatMessageDaoFactory;
import com.health.companion.di.DatabaseModule_ProvideConversationDaoFactory;
import com.health.companion.di.DatabaseModule_ProvideDatabaseFactory;
import com.health.companion.di.DatabaseModule_ProvideDocumentDaoFactory;
import com.health.companion.di.DatabaseModule_ProvideHealthMetricDaoFactory;
import com.health.companion.di.DatabaseModule_ProvideMoodEntryDaoFactory;
import com.health.companion.di.MLModule_ProvideVoiceInputManagerFactory;
import com.health.companion.di.NetworkModule_ProvideAuthApiFactory;
import com.health.companion.di.NetworkModule_ProvideAuthInterceptorFactory;
import com.health.companion.di.NetworkModule_ProvideChatApiFactory;
import com.health.companion.di.NetworkModule_ProvideDashboardApiFactory;
import com.health.companion.di.NetworkModule_ProvideDocumentApiFactory;
import com.health.companion.di.NetworkModule_ProvideHealthApiFactory;
import com.health.companion.di.NetworkModule_ProvideJsonFactory;
import com.health.companion.di.NetworkModule_ProvideLoggingInterceptorFactory;
import com.health.companion.di.NetworkModule_ProvideOkHttpClientFactory;
import com.health.companion.di.NetworkModule_ProvideProfileApiFactory;
import com.health.companion.di.NetworkModule_ProvideRetrofitFactory;
import com.health.companion.di.NetworkModule_ProvideTokenAuthenticatorFactory;
import com.health.companion.di.NetworkModule_ProvideVoiceApiFactory;
import com.health.companion.di.RepositoryModule_ProvideAuthRepositoryFactory;
import com.health.companion.di.RepositoryModule_ProvideChatRepositoryFactory;
import com.health.companion.di.RepositoryModule_ProvideDashboardRepositoryFactory;
import com.health.companion.di.RepositoryModule_ProvideDocumentRepositoryFactory;
import com.health.companion.di.RepositoryModule_ProvideHealthRepositoryFactory;
import com.health.companion.di.RepositoryModule_ProvideProfileRepositoryFactory;
import com.health.companion.ml.voice.VoiceInputManager;
import com.health.companion.presentation.screens.auth.AuthViewModel;
import com.health.companion.presentation.screens.auth.AuthViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.chat.ChatViewModel;
import com.health.companion.presentation.screens.chat.ChatViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.dashboard.DashboardViewModel;
import com.health.companion.presentation.screens.dashboard.DashboardViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.documents.DocumentsViewModel;
import com.health.companion.presentation.screens.documents.DocumentsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.health.HealthViewModel;
import com.health.companion.presentation.screens.health.HealthViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.mood.MoodViewModel;
import com.health.companion.presentation.screens.mood.MoodViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.profile.ProfileViewModel;
import com.health.companion.presentation.screens.profile.ProfileViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.presentation.screens.settings.SettingsViewModel;
import com.health.companion.presentation.screens.settings.SettingsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.health.companion.services.WebSocketManager;
import com.health.companion.utils.TokenManager;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SetBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DaggerApp_HiltComponents_SingletonC {
  private DaggerApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public App_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements App_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public App_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements App_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public App_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements App_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public App_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements App_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public App_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements App_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public App_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements App_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public App_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements App_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public App_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends App_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends App_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends App_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends App_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Set<String> getViewModelKeys() {
      return SetBuilder.<String>newSetBuilder(8).add(AuthViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ChatViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(DashboardViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(DocumentsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(HealthViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(MoodViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ProfileViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(SettingsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).build();
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectTokenManager(instance, singletonCImpl.tokenManagerProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends App_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<DocumentsViewModel> documentsViewModelProvider;

    private Provider<HealthViewModel> healthViewModelProvider;

    private Provider<MoodViewModel> moodViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.documentsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.healthViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.moodViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
    }

    @Override
    public Map<String, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(8).put("com.health.companion.presentation.screens.auth.AuthViewModel", ((Provider) authViewModelProvider)).put("com.health.companion.presentation.screens.chat.ChatViewModel", ((Provider) chatViewModelProvider)).put("com.health.companion.presentation.screens.dashboard.DashboardViewModel", ((Provider) dashboardViewModelProvider)).put("com.health.companion.presentation.screens.documents.DocumentsViewModel", ((Provider) documentsViewModelProvider)).put("com.health.companion.presentation.screens.health.HealthViewModel", ((Provider) healthViewModelProvider)).put("com.health.companion.presentation.screens.mood.MoodViewModel", ((Provider) moodViewModelProvider)).put("com.health.companion.presentation.screens.profile.ProfileViewModel", ((Provider) profileViewModelProvider)).put("com.health.companion.presentation.screens.settings.SettingsViewModel", ((Provider) settingsViewModelProvider)).build();
    }

    @Override
    public Map<String, Object> getHiltViewModelAssistedMap() {
      return Collections.<String, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.health.companion.presentation.screens.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 1: // com.health.companion.presentation.screens.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideDocumentRepositoryProvider.get(), singletonCImpl.provideVoiceInputManagerProvider.get(), singletonCImpl.voiceRepositoryProvider.get(), singletonCImpl.tokenManagerProvider.get(), viewModelCImpl.savedStateHandle);

          case 2: // com.health.companion.presentation.screens.dashboard.DashboardViewModel 
          return (T) new DashboardViewModel(singletonCImpl.provideDashboardRepositoryProvider.get());

          case 3: // com.health.companion.presentation.screens.documents.DocumentsViewModel 
          return (T) new DocumentsViewModel(singletonCImpl.provideDocumentRepositoryProvider.get(), singletonCImpl.tokenManagerProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.health.companion.presentation.screens.health.HealthViewModel 
          return (T) new HealthViewModel(singletonCImpl.provideHealthRepositoryProvider.get());

          case 5: // com.health.companion.presentation.screens.mood.MoodViewModel 
          return (T) new MoodViewModel(singletonCImpl.provideHealthRepositoryProvider.get());

          case 6: // com.health.companion.presentation.screens.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.provideProfileRepositoryProvider.get());

          case 7: // com.health.companion.presentation.screens.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.tokenManagerProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends App_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends App_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends App_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<DataStore<Preferences>> providePreferencesDataStoreProvider;

    private Provider<TokenManager> tokenManagerProvider;

    private Provider<HttpLoggingInterceptor> provideLoggingInterceptorProvider;

    private Provider<Interceptor> provideAuthInterceptorProvider;

    private Provider<TokenAuthenticator> provideTokenAuthenticatorProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<AuthApi> provideAuthApiProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<ChatApi> provideChatApiProvider;

    private Provider<HealthCompanionDatabase> provideDatabaseProvider;

    private Provider<ChatMessageDao> provideChatMessageDaoProvider;

    private Provider<ConversationDao> provideConversationDaoProvider;

    private Provider<WebSocketManager> webSocketManagerProvider;

    private Provider<ChatRepository> provideChatRepositoryProvider;

    private Provider<DocumentApi> provideDocumentApiProvider;

    private Provider<DocumentDao> provideDocumentDaoProvider;

    private Provider<DocumentRepository> provideDocumentRepositoryProvider;

    private Provider<VoiceInputManager> provideVoiceInputManagerProvider;

    private Provider<VoiceApi> provideVoiceApiProvider;

    private Provider<VoiceRepository> voiceRepositoryProvider;

    private Provider<DashboardApi> provideDashboardApiProvider;

    private Provider<DashboardRepository> provideDashboardRepositoryProvider;

    private Provider<HealthApi> provideHealthApiProvider;

    private Provider<HealthMetricDao> provideHealthMetricDaoProvider;

    private Provider<MoodEntryDao> provideMoodEntryDaoProvider;

    private Provider<HealthRepository> provideHealthRepositoryProvider;

    private Provider<ProfileApi> provideProfileApiProvider;

    private Provider<ProfileRepository> provideProfileRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.providePreferencesDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 1));
      this.tokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<TokenManager>(singletonCImpl, 0));
      this.provideLoggingInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<HttpLoggingInterceptor>(singletonCImpl, 6));
      this.provideAuthInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<Interceptor>(singletonCImpl, 7));
      this.provideTokenAuthenticatorProvider = DoubleCheck.provider(new SwitchingProvider<TokenAuthenticator>(singletonCImpl, 8));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 5));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 9));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 4));
      this.provideAuthApiProvider = DoubleCheck.provider(new SwitchingProvider<AuthApi>(singletonCImpl, 3));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 2));
      this.provideChatApiProvider = DoubleCheck.provider(new SwitchingProvider<ChatApi>(singletonCImpl, 11));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<HealthCompanionDatabase>(singletonCImpl, 13));
      this.provideChatMessageDaoProvider = DoubleCheck.provider(new SwitchingProvider<ChatMessageDao>(singletonCImpl, 12));
      this.provideConversationDaoProvider = DoubleCheck.provider(new SwitchingProvider<ConversationDao>(singletonCImpl, 14));
      this.webSocketManagerProvider = DoubleCheck.provider(new SwitchingProvider<WebSocketManager>(singletonCImpl, 15));
      this.provideChatRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepository>(singletonCImpl, 10));
      this.provideDocumentApiProvider = DoubleCheck.provider(new SwitchingProvider<DocumentApi>(singletonCImpl, 17));
      this.provideDocumentDaoProvider = DoubleCheck.provider(new SwitchingProvider<DocumentDao>(singletonCImpl, 18));
      this.provideDocumentRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DocumentRepository>(singletonCImpl, 16));
      this.provideVoiceInputManagerProvider = DoubleCheck.provider(new SwitchingProvider<VoiceInputManager>(singletonCImpl, 19));
      this.provideVoiceApiProvider = DoubleCheck.provider(new SwitchingProvider<VoiceApi>(singletonCImpl, 21));
      this.voiceRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<VoiceRepository>(singletonCImpl, 20));
      this.provideDashboardApiProvider = DoubleCheck.provider(new SwitchingProvider<DashboardApi>(singletonCImpl, 23));
      this.provideDashboardRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DashboardRepository>(singletonCImpl, 22));
      this.provideHealthApiProvider = DoubleCheck.provider(new SwitchingProvider<HealthApi>(singletonCImpl, 25));
      this.provideHealthMetricDaoProvider = DoubleCheck.provider(new SwitchingProvider<HealthMetricDao>(singletonCImpl, 26));
      this.provideMoodEntryDaoProvider = DoubleCheck.provider(new SwitchingProvider<MoodEntryDao>(singletonCImpl, 27));
      this.provideHealthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<HealthRepository>(singletonCImpl, 24));
      this.provideProfileApiProvider = DoubleCheck.provider(new SwitchingProvider<ProfileApi>(singletonCImpl, 29));
      this.provideProfileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ProfileRepository>(singletonCImpl, 28));
    }

    @Override
    public void injectApp(App app) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.health.companion.utils.TokenManager 
          return (T) new TokenManager(singletonCImpl.providePreferencesDataStoreProvider.get());

          case 1: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvidePreferencesDataStoreFactory.providePreferencesDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.health.companion.data.repositories.AuthRepository 
          return (T) RepositoryModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideAuthApiProvider.get(), singletonCImpl.tokenManagerProvider.get());

          case 3: // com.health.companion.data.remote.api.AuthApi 
          return (T) NetworkModule_ProvideAuthApiFactory.provideAuthApi(singletonCImpl.provideRetrofitProvider.get());

          case 4: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 5: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient(singletonCImpl.provideLoggingInterceptorProvider.get(), singletonCImpl.provideAuthInterceptorProvider.get(), singletonCImpl.provideTokenAuthenticatorProvider.get());

          case 6: // okhttp3.logging.HttpLoggingInterceptor 
          return (T) NetworkModule_ProvideLoggingInterceptorFactory.provideLoggingInterceptor();

          case 7: // okhttp3.Interceptor 
          return (T) NetworkModule_ProvideAuthInterceptorFactory.provideAuthInterceptor(singletonCImpl.tokenManagerProvider.get());

          case 8: // com.health.companion.data.remote.TokenAuthenticator 
          return (T) NetworkModule_ProvideTokenAuthenticatorFactory.provideTokenAuthenticator(singletonCImpl.tokenManagerProvider.get());

          case 9: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 10: // com.health.companion.data.repositories.ChatRepository 
          return (T) RepositoryModule_ProvideChatRepositoryFactory.provideChatRepository(singletonCImpl.provideChatApiProvider.get(), singletonCImpl.provideChatMessageDaoProvider.get(), singletonCImpl.provideConversationDaoProvider.get(), singletonCImpl.webSocketManagerProvider.get(), singletonCImpl.tokenManagerProvider.get(), singletonCImpl.provideOkHttpClientProvider.get());

          case 11: // com.health.companion.data.remote.api.ChatApi 
          return (T) NetworkModule_ProvideChatApiFactory.provideChatApi(singletonCImpl.provideRetrofitProvider.get());

          case 12: // com.health.companion.data.local.dao.ChatMessageDao 
          return (T) DatabaseModule_ProvideChatMessageDaoFactory.provideChatMessageDao(singletonCImpl.provideDatabaseProvider.get());

          case 13: // com.health.companion.data.local.database.HealthCompanionDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 14: // com.health.companion.data.local.dao.ConversationDao 
          return (T) DatabaseModule_ProvideConversationDaoFactory.provideConversationDao(singletonCImpl.provideDatabaseProvider.get());

          case 15: // com.health.companion.services.WebSocketManager 
          return (T) new WebSocketManager(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.tokenManagerProvider.get());

          case 16: // com.health.companion.data.repositories.DocumentRepository 
          return (T) RepositoryModule_ProvideDocumentRepositoryFactory.provideDocumentRepository(singletonCImpl.provideDocumentApiProvider.get(), singletonCImpl.provideDocumentDaoProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 17: // com.health.companion.data.remote.api.DocumentApi 
          return (T) NetworkModule_ProvideDocumentApiFactory.provideDocumentApi(singletonCImpl.provideRetrofitProvider.get());

          case 18: // com.health.companion.data.local.dao.DocumentDao 
          return (T) DatabaseModule_ProvideDocumentDaoFactory.provideDocumentDao(singletonCImpl.provideDatabaseProvider.get());

          case 19: // com.health.companion.ml.voice.VoiceInputManager 
          return (T) MLModule_ProvideVoiceInputManagerFactory.provideVoiceInputManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 20: // com.health.companion.data.repositories.VoiceRepository 
          return (T) new VoiceRepository(singletonCImpl.provideVoiceApiProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 21: // com.health.companion.data.remote.api.VoiceApi 
          return (T) NetworkModule_ProvideVoiceApiFactory.provideVoiceApi(singletonCImpl.provideRetrofitProvider.get());

          case 22: // com.health.companion.data.repositories.DashboardRepository 
          return (T) RepositoryModule_ProvideDashboardRepositoryFactory.provideDashboardRepository(singletonCImpl.provideDashboardApiProvider.get());

          case 23: // com.health.companion.data.remote.api.DashboardApi 
          return (T) NetworkModule_ProvideDashboardApiFactory.provideDashboardApi(singletonCImpl.provideRetrofitProvider.get());

          case 24: // com.health.companion.data.repositories.HealthRepository 
          return (T) RepositoryModule_ProvideHealthRepositoryFactory.provideHealthRepository(singletonCImpl.provideHealthApiProvider.get(), singletonCImpl.provideHealthMetricDaoProvider.get(), singletonCImpl.provideMoodEntryDaoProvider.get());

          case 25: // com.health.companion.data.remote.api.HealthApi 
          return (T) NetworkModule_ProvideHealthApiFactory.provideHealthApi(singletonCImpl.provideRetrofitProvider.get());

          case 26: // com.health.companion.data.local.dao.HealthMetricDao 
          return (T) DatabaseModule_ProvideHealthMetricDaoFactory.provideHealthMetricDao(singletonCImpl.provideDatabaseProvider.get());

          case 27: // com.health.companion.data.local.dao.MoodEntryDao 
          return (T) DatabaseModule_ProvideMoodEntryDaoFactory.provideMoodEntryDao(singletonCImpl.provideDatabaseProvider.get());

          case 28: // com.health.companion.data.repositories.ProfileRepository 
          return (T) RepositoryModule_ProvideProfileRepositoryFactory.provideProfileRepository(singletonCImpl.provideProfileApiProvider.get());

          case 29: // com.health.companion.data.remote.api.ProfileApi 
          return (T) NetworkModule_ProvideProfileApiFactory.provideProfileApi(singletonCImpl.provideRetrofitProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
